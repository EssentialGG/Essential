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
package gg.essential.util

import gg.essential.data.VersionInfo
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.image.BlurHashImage
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.width
import gg.essential.handlers.CertChain
import gg.essential.lib.caffeine.cache.CacheLoader
import gg.essential.lib.caffeine.cache.Caffeine
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

object CachedAvatarImage {
    private val LOGGER = LoggerFactory.getLogger(CachedAvatarImage::class.java)

    private val cacheBasePath = platform.essentialBaseDir.resolve("avatar-cache")
    private val cachePath = object {
        operator fun get(uuid: UUID): Path = uuid.toDashlessString().let {
            cacheBasePath
                .resolve(it[0].toString())
                .resolve(it[1].toString())
                .resolve(it.substring(2))
        }
    }

    /** Cache which supplies fresh avatars no older than 30 minutes. */
    private val freshCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .executor(Dispatchers.IO.asExecutor())
        .buildAsync<UUID, BufferedImage>(CacheLoader {
            loadFromWeb(it)
        })

    /** Cache which supplies avatars from disk, regardless of age. */
    private val diskCache = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .executor(Dispatchers.IO.asExecutor())
        .buildAsync<UUID, Optional<BufferedImage>>(CacheLoader {
            Optional.ofNullable(loadFromDisk(it))
        })

    private val fallbackImage = BlurHashImage("U9QuA+e8vyu48wVsVYkq_~tlP9Z~Y7pIyXVX")

    private val httpClient by lazy { createHttpClient() }

    private fun loadFromDisk(uuid: UUID): BufferedImage? {
        val path = cachePath[uuid]
        if (!path.exists()) {
            return null
        }
        return try {
            ImageIO.read(path.toFile())
        } catch (e: Exception) {
            LOGGER.warn("Failed to load avatar $uuid from cache: ", e)
            null
        }
    }

    private fun loadFromWeb(uuid: UUID): BufferedImage? {
        val url = "https://crafthead.net/helm/${uuid.toDashlessString()}"

        val bytes = try {
            val request = HttpGet(url)
            val response = httpClient.execute(request)
            val content = response.entity.content.use { it.readBytes() }
            if (response.statusLine.statusCode != 200) {
                LOGGER.warn("Non-OK status code received when fetching $url: ${content.toString(Charsets.UTF_8)}")
                return null
            }

            content
        } catch (e: Exception) {
            LOGGER.warn("Failed to fetch $url: ", e)
            return null
        }

        val image = try {
            ImageIO.read(bytes.inputStream())
        } catch (e: Exception) {
            LOGGER.warn("Failed read avatar $uuid image: ", e)
            return null
        }

        diskCache.asMap()[uuid] = completedFuture(Optional.of(image))

        try {
            val path = cachePath[uuid]
            path.parent.createDirectories()
            path.writeBytes(bytes)
        } catch (e: Exception) {
            LOGGER.warn("Failed to write avatar $uuid cache: ", e)
            return null
        }

        return image
    }

    private fun createHttpClient(): CloseableHttpClient {
        val (sslContext, _) = CertChain()
            .loadEmbedded()
            .done()

        return HttpClientBuilder
            .create()
            .setUserAgent("Essential/${VersionInfo().essentialVersion} (https://essential.gg)")
            .setSslcontext(sslContext)
            .build()
    }

    /**
     * Creates a [UIImage] component that will contain the head of the given user's skin for use as an avatar image.
     *
     * The image will be cached for half an hour. If the cached image has expired, it will still be used as the loading
     * image until a new one has been retrieved.
     * If no old image is available (or not yet read from disk), a [BlurHashImage] is used as fallback.
     */
    @JvmStatic
    @Deprecated("Use create(uuid) instead", ReplaceWith("create(uuid)"))
    fun ofUUID(uuid: UUID): UIImage {
        // The extra thenApply is required because UIImage calls obtrudeValue(null)
        val freshFuture = freshCache[uuid].thenApply { it }
        if (freshFuture.isDone) {
            return UIImage(freshFuture.toCompletableFuture(), fallbackImage)
        }
        val diskFuture = diskCache[uuid].thenCompose {
            it.map(::completedFuture).orElse(freshFuture)
        }
        return UIImage(freshFuture, UIImage(diskFuture, fallbackImage))
    }

    /**
     * Creates a [UIComponent] that will contain the head of the given user's skin for use as an avatar image.
     *
     * This component also gives a solid shadow when required by wrapping the UIImage in a UIContainer.
     *
     * The image will be cached for half an hour. If the cached image has expired, it will still be used as the loading
     * image until a new one has been retrieved.
     * If no old image is available (or not yet read from disk), a [BlurHashImage] is used as fallback.
     */
    @JvmStatic
    fun create(uuid: UUID): UIComponent {
        // The extra thenApply is required because UIImage calls obtrudeValue(null)
        val freshFuture = freshCache[uuid].thenApply { it }
        val image = if (freshFuture.isDone) {
            UIImage(freshFuture.toCompletableFuture(), fallbackImage)
        } else {
            val diskFuture = diskCache[uuid].thenCompose {
                it.map(::completedFuture).orElse(freshFuture)
            }
            UIImage(freshFuture, UIImage(diskFuture, fallbackImage))
        }
        val uiContainer = UIContainer()
        uiContainer.layout(Modifier.width(8f).heightAspect(1f)) {
            image(Modifier.fillParent())
        }
        return uiContainer
    }

    private fun UUID.toDashlessString() = toString().replace("-", "")
}
