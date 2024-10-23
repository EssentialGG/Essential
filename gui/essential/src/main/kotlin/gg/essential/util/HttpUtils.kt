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
@file:JvmName("HttpUtils")
package gg.essential.util

import gg.essential.data.VersionInfo
import gg.essential.handlers.CertChain
import kotlinx.coroutines.future.asDeferred
import okhttp3.OkHttpClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509TrustManager

val httpClient: CompletableFuture<OkHttpClient> = CompletableFuture.supplyAsync {
    val (sslContext, trustManagers) = CertChain().loadEmbedded().done()
    val trustManager = trustManagers[0] as X509TrustManager
    OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor {
            it.proceed(
                it.request().newBuilder()
                    .header("User-Agent", "Essential/${VersionInfo().essentialVersion} (https://essential.gg)")
                    .build()
            )
        }.build()
}

private val httpClientDeferred = httpClient.asDeferred()

suspend fun httpClient(): OkHttpClient =
    httpClientDeferred.await()

