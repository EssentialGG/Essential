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
package gg.essential.gui.screenshot.handler

import com.google.common.collect.Sets
import com.sparkuniverse.toolbox.serialization.DateTimeTypeAdapter
import com.sparkuniverse.toolbox.serialization.UUIDTypeAdapter
import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.Essential
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.lib.gson.GsonBuilder
import gg.essential.lib.gson.JsonSyntaxException
import gg.essential.network.connectionmanager.media.ScreenshotManager
import gg.essential.util.UUIDUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ScreenshotMetadataManager(
    private val metadataFolder: File,
    private val screenshotChecksumManager: ScreenshotChecksumManager,
) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
        .registerTypeAdapter(DateTime::class.java, DateTimeTypeAdapter())
        .create()

    private val metadataCache: MutableMap<String, ClientScreenshotMetadata> = ConcurrentHashMap()

    // Screenshot checksums which have no metadata
    private val negativeChecksumCache = Sets.newConcurrentHashSet<String>()

    fun updateMetadata(screenshotMetadata: ClientScreenshotMetadata) {
        metadataCache[screenshotMetadata.checksum] = screenshotMetadata
        writeMetadata(screenshotMetadata)
    }

    private fun readMetadata(imageChecksum: String): ClientScreenshotMetadata? {
        return try {
            val fileContents = File(metadataFolder, imageChecksum).readText()
            gson.fromJson(fileContents, ClientScreenshotMetadata::class.java)
        } catch (exception: JsonSyntaxException) {
            Essential.logger.error("Metadata corrupt for checksum $imageChecksum. Attempting recovery.", exception)
            tryRecoverMetadata(imageChecksum)
        } catch (ignored: FileNotFoundException) {
            null
        }
    }

    /**
     * Attempts to recover metadata from cached metadata. If that fails, attempts to create new metadata if the metadata file exists.
     * @return  The recovered [ClientScreenshotMetadata] if it exists in the cache, new metadata if the metadata file exists, or null if neither exist
     */
    private fun tryRecoverMetadata(checksum: String): ClientScreenshotMetadata? {
        val metadata = screenshotChecksumManager.getPathsForChecksum(checksum).firstOrNull()?.let {
            createMetadata(ScreenshotManager.getImageTime(it, null, false), checksum)
        }
        metadata?.let { writeMetadata(it) }
        return metadata
    }

    private fun getMetadata(checksum: String): ClientScreenshotMetadata? {
        if (negativeChecksumCache.contains(checksum)) {
            return null
        }
        val metadata = metadataCache.compute(checksum) { _, it -> it ?: readMetadata(checksum) }
        if (metadata == null) {
            negativeChecksumCache.add(checksum)
        }
        return metadata
    }

    fun getMetadata(path: Path): ClientScreenshotMetadata? {
        return getMetadata(path.toFile())
    }

    fun getMetadata(file: File): ClientScreenshotMetadata? {
        val imageChecksum = screenshotChecksumManager[file] ?: return null

        return getMetadata(imageChecksum)
    }

    /**
     * Get metadata straight from the cache using a media id.
     */
    fun getMetadataCache(mediaId: String): ClientScreenshotMetadata? {
        return metadataCache.values.firstOrNull { it.mediaId == mediaId }
    }

    private fun writeMetadata(metadata: ClientScreenshotMetadata) {
        negativeChecksumCache.remove(metadata.checksum)
        try {
            File(metadataFolder, metadata.checksum).writeText(gson.toJson(metadata))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Called when a screenshot file is deleted externally
     * such as the user deleting it in their file manager
     */
    fun handleExternalDelete(fileName: String) {
        val checksum = screenshotChecksumManager.remove(fileName) ?: return
        val metadata = getMetadata(checksum)
        if (metadata != null) {
            deleteMetadata(metadata)
        }
    }

    private fun deleteMetadata(metadata: ClientScreenshotMetadata) {
        val metadataFile = File(metadataFolder, metadata.checksum)
        metadataCache.remove(metadata.checksum)
        FileUtils.deleteQuietly(metadataFile)
    }

    fun deleteMetadata(file: File) {
        val metadata = getMetadata(file)
        if (metadata != null) {
            deleteMetadata(metadata)
            screenshotChecksumManager.delete(file)
        }
    }

    fun createMetadata(time: DateTime, checksum: String): ClientScreenshotMetadata {
        return ClientScreenshotMetadata(
            UUIDUtil.getClientUUID(),
            time,
            checksum,
            null,
            ClientScreenshotMetadata.Location(ClientScreenshotMetadata.Location.Type.UNKNOWN, "Unknown"),
            favorite = false,
            edited = false
        )
    }

    @Synchronized
    fun getOrCreateMetadata(path: Path): ClientScreenshotMetadata {
        val file = path.toFile()

        val existingMetadata = getMetadata(file)
        if (existingMetadata != null) {
            return existingMetadata
        }

        val checksum = screenshotChecksumManager[file] ?: throw IllegalStateException("No checksum for file $file. Was the file deleted?")

        return createMetadata(ScreenshotManager.getImageTime(path, null, false), checksum).also {
            updateMetadata(it)
        }
    }
}