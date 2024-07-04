/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.gui.account

import com.mojang.authlib.GameProfile
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.exceptions.InvalidCredentialsException
import gg.essential.handlers.CertChain
import gg.essential.handlers.account.WebAccountManager
import gg.essential.lib.gson.*
import gg.essential.lib.gson.annotations.JsonAdapter
import gg.essential.util.UUIDUtil
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import java.io.IOException
import java.lang.reflect.Type
import java.net.URI
import java.net.URLEncoder
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KMutableProperty0

class MicrosoftUserAuthentication {
    companion object {
        private const val DEBUG = false
        const val CLIENT_ID = "e39cc675-eb52-4475-b5f8-82aaae14eeba"
        private const val SCOPE = "Xboxlive.signin Xboxlive.offline_access"
        private const val URL_OAUTH_START = "https://login.live.com/oauth20_authorize.srf"
        private const val URL_OAUTH_TOKEN = "https://login.live.com/oauth20_token.srf"
        private const val URL_XBL = "https://user.auth.xboxlive.com/user/authenticate"
        private const val URL_XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val URL_MINECRAFT = "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val URL_PROFILE = "https://api.minecraftservices.com/minecraft/profile"
    }


    private var redirectUri: String? = null
    private var accessToken: Token? = null
    @field:JsonAdapter(LegacyTokenSerializer::class)
    private var refreshToken: Token? = null
    private var xblToken: Token? = null
    private var xstsToken: Token? = null
    private var uhs: String? = null
    private var mcToken: Token? = null
    private var profile: GameProfile? = null
    val expiryTime: Instant?
        get() = refreshToken?.expires
    var openUri: URI? = null

    fun logIn(future: CompletableFuture<URI>, forceRefresh: Boolean = false): Pair<GameProfile, String> {
        if (forceRefresh) {
            mcToken = null
        }
        val profile = acquireGameProfile(future)
        val token = acquireMCToken(future)
        return Pair(profile, token)
    }

    fun refreshRefreshToken() {
        acquireAccessToken(CompletableFuture.completedFuture(null), true)
    }

    private fun acquireAccessToken(future: CompletableFuture<URI>, forceRefresh: Boolean): String {
        if (!forceRefresh) {
            accessToken.ifValid { return it }
        }

        val refreshToken = this.refreshToken ?: return acquireAccessTokenViaOAuth(future)
        // https://wiki.vg/Microsoft_Authentication_Scheme#Refreshing_Tokens
        return fetchAccessToken(
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken.value
            )
        )
    }

    private fun acquireAccessTokenViaOAuth(future: CompletableFuture<URI>): String {
        val codeVerifier = Base64.encodeBase64URLSafeString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        val code = acquireAuthorizationCode(codeVerifier, future)
        // https://wiki.vg/Microsoft_Authentication_Scheme#Authorization_Code_-.3E_Authorization_Token
        return fetchAccessToken(
            mapOf(
                "code_verifier" to codeVerifier,
                "grant_type" to "authorization_code",
                "code" to code
            )
        )
    }

    // https://wiki.vg/Microsoft_Authentication_Scheme#Microsoft_OAuth_Flow
    private fun acquireAuthorizationCode(codeVerifier: String, future: CompletableFuture<URI>): String {
        //Get future from account manager instead of ourselves

        redirectUri = WebAccountManager.microsoftRedirectUri

        val uri = acquireUri(codeVerifier)
        if (future.isDone) {
            throw InvalidCredentialsException("Re-authentication with Microsoft required")
        }
        future.complete(uri)
        openUri = uri
        return WebAccountManager.authorizationCodeFuture?.join()!!
    }

    private fun acquireUri(codeVerifier: String): URI {
        return URI(
            "$URL_OAUTH_START?" + mapOf(
                "client_id" to CLIENT_ID,
                "prompt" to "select_account",
                "scope" to SCOPE,
                "code_challenge_method" to "S256",
                "code_challenge" to Base64.encodeBase64URLSafeString(DigestUtils.sha256(codeVerifier)),
                "response_type" to "code",
                "redirect_uri" to redirectUri
            ).map { (key, value) -> "$key=${value?.urlEncode()}" }.joinToString("&")
        )
    }

    private fun fetchAccessToken(props: Map<String, String>): String {
        val fullProps = props + mapOf(
            "client_id" to CLIENT_ID,
            "scope" to SCOPE,
            "redirect_uri" to (redirectUri ?: "")
        )
        val response = HttpPost(URL_OAUTH_TOKEN).apply {
            setHeader("Content-Type", "application/x-www-form-urlencoded")
            entity = UrlEncodedFormEntity(fullProps.map { (name, value) -> BasicNameValuePair(name, value) })
        }.execute() ?: throw InvalidCredentialsException()
        val accessToken = response["access_token"].asString
        val refreshToken = response["refresh_token"].asString
        val expiresIn = response["expires_in"].asLong
        this.accessToken = Token(accessToken, Instant.now() + Duration.ofSeconds(expiresIn))
        //Refresh tokens last 90 days https://docs.microsoft.com/en-us/azure/active-directory/develop/refresh-tokens
        this.refreshToken = Token(refreshToken, Instant.now() + Duration.ofDays(90))
        return accessToken
    }

    // https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XBL
    private fun acquireXBLToken(future: CompletableFuture<URI>, retry: Boolean = true): String {
        xblToken.ifValid { return it }

        val accessToken = acquireAccessToken(future, false)

        val response = post(
            URL_XBL, mapOf(
                "Properties" to mapOf(
                    "AuthMethod" to "RPS",
                    "SiteName" to "user.auth.xboxlive.com",
                    "RpsTicket" to "d=$accessToken"
                ),
                "RelyingParty" to "http://auth.xboxlive.com",
                "TokenType" to "JWT"
            )
        ) ?: return retry.attempt(this::accessToken) { this.acquireXBLToken(future, it) }
        val token = response["Token"].asString
        val expires = Instant.parse(response["NotAfter"].asString)
        this.xblToken = Token(token, expires)
        return token
    }

    // https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XSTS
    private fun acquireXSTSToken(future: CompletableFuture<URI>, retry: Boolean = true): Pair<String, String> {
        xstsToken.ifValid { return Pair(it, uhs!!) }

        val xblToken = acquireXBLToken(future)

        val response = post(
            URL_XSTS, mapOf(
                "Properties" to mapOf(
                    "SandboxId" to "RETAIL",
                    "UserTokens" to listOf(xblToken)
                ),
                "RelyingParty" to "rp://api.minecraftservices.com/",
                "TokenType" to "JWT"
            )
        ) ?: return retry.attempt(this::xblToken) { this.acquireXSTSToken(future, it) }
        val token = response["Token"].asString
        val expires = Instant.parse(response["NotAfter"].asString)
        val uhs = response["DisplayClaims"].asJsonObject["xui"].asJsonArray[0].asJsonObject["uhs"].asString
        this.xstsToken = Token(token, expires)
        this.uhs = uhs
        return Pair(token, uhs)
    }

    // https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_Minecraft
    private fun acquireMCToken(future: CompletableFuture<URI>, retry: Boolean = true): String {
        mcToken.ifValid { return it }

        val (xstsToken, uhs) = acquireXSTSToken(future)

        val response = post(
            URL_MINECRAFT, mapOf(
                "identityToken" to "XBL3.0 x=$uhs;$xstsToken"
            )
        ) ?: return retry.attempt(this::xstsToken) { this.acquireMCToken(future, it) }
        val token = response["access_token"].asString
        val expiresIn = response["expires_in"].asLong
        mcToken = Token(token, Instant.now() + Duration.ofSeconds(expiresIn))
        profile = null // flush cache
        return token
    }

    // https://wiki.vg/Microsoft_Authentication_Scheme#Get_the_profile
    private fun acquireGameProfile(future: CompletableFuture<URI>): GameProfile {
        // We acquire the token before checking the cache
        // so we regularly refresh the profile.
        val token = acquireMCToken(future)

        profile?.let { return it }

        val response = HttpGet(URL_PROFILE).apply {
            setHeader("Authorization", "Bearer $token")
        }.execute() ?: throw InvalidCredentialsException()
        response["error"]?.asString?.let {
            if (it == "NOT_FOUND") {
                // We use this response as an indicator for whether the given account
                // owns the game. We can do this because we always fetch the profile
                // right after fetching a new access token, so the token ought to be valid.
                throw InvalidCredentialsException("This account does not own Minecraft")
            } else {
                println(Gson().toJson(response))
                throw IOException(response["errorMessage"].asString)
            }
        }
        val uuid = UUIDUtil.formatWithDashes(response["id"].asString)
        val name = response["name"].asString
        val profile = GameProfile(uuid, name)
        this.profile = profile
        return profile
    }

    private fun post(uri: String, content: Any): JsonObject? {
        val request = HttpPost(uri)
        request.entity = EntityBuilder.create().apply {
            text = Gson().toJson(content)
            contentType = ContentType.APPLICATION_JSON
        }.build()
        return request.execute()
    }

    private fun HttpUriRequest.execute(): JsonObject? {
        setHeader("Accept", "application/json")
        val response = createHttpClient().execute(this)
        val status = response.statusLine.statusCode
        val content = response.entity.content.use { it.bufferedReader().readText() }
        if (DEBUG || status >= 300) {
            println("$status: $content")
        }
        val json = Gson().fromJson(content, JsonObject::class.java)
        if (DEBUG || json.has("error")) {
            println(GsonBuilder().setPrettyPrinting().create().toJson(json))
        }
        // MS APIs return 400 on invalid token, Mojang returns 401, 429 for rate limit
        return if (status == 400 || status == 401 || status == 429) null else json
    }

    private fun createHttpClient(): CloseableHttpClient {
        // Microsoft is transitioning their certificates to other root CAs because the current one expires in 2025.
        // https://docs.microsoft.com/en-us/azure/security/fundamentals/tls-certificate-changes
        val (sslContext, _) = CertChain()
            .loadEmbedded()
            .done()
        return HttpClientBuilder.create()
            .setSslcontext(sslContext)
            .build()
    }

    private fun String.urlEncode() = URLEncoder.encode(this, "utf-8")

    // retry.attempt
    private fun <P, T> Boolean.attempt(invalidToken: KMutableProperty0<P?>, func: (retry: Boolean) -> T): T =
        if (this) {
            invalidToken.set(null)
            func(false)
        } else {
            throw AuthenticationUnavailableException()
        }

    data class Token(val value: String, val expires: Instant) {
        val expired: Boolean
            get() = expires.isBefore(Instant.now())
    }

    private inline fun Token?.ifValid(block: (token: String) -> Unit) {
        if (this != null && !this.expired) {
            block(value)
        }
    }

    private class LegacyTokenSerializer : JsonSerializer<Token>, JsonDeserializer<Token> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Token {
            return when (json) {
                is JsonPrimitive -> Token(json.asString, Instant.now())
                else -> context.deserialize(json, Token::class.java)
            }
        }

        override fun serialize(src: Token, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            context.serialize(src)
    }
}
