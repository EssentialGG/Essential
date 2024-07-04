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
package gg.essential.api.utils

import org.apache.commons.io.IOUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/**
 * Utility for fetching data from the internet.
 */
object WebUtil {
    /**
     * Log information and errors to the console.
     */
    @JvmStatic
    var LOG = false

    /**
     * Fetch a JSON object from the internet.
     *
     * @param url JSON location
     * @return JSON as a Sk1er [JsonHolder]
     * @see JsonHolder
     */
    @JvmStatic
    fun fetchJSON(url: String): JsonHolder = JsonHolder(fetchString(url))

    /**
     * Fetch the content of a web page.
     *
     * @param url page location
     * @return page content
     */
    @JvmStatic
    fun fetchString(url: String): String? {
        val escapedUrl = url.replace(" ", "%20")
        if (LOG) println("Fetching $escapedUrl")
        try {
            setup(escapedUrl, "Mozilla/4.76 (Essential)").use { setup ->
                return IOUtils.toString(setup, Charset.defaultCharset())
            }
        } catch (e: Exception) {
            println("Failed to fetch from $url")
            e.printStackTrace()
        }
        return "Failed to fetch"
    }

    /**
     * Download a file from the internet.
     *
     * @param url file location
     * @param file location to save the file
     * @param userAgent user-agent to use for fetching the file
     */
    @JvmStatic
    @Throws(IOException::class)
    fun downloadToFile(url: String, file: File, userAgent: String) {
        FileOutputStream(file).use { output ->
            BufferedInputStream(setup(url, userAgent)).use { input ->
                val dataBuffer = ByteArray(1024)
                var bytesRead: Int
                while (input.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                    output.write(dataBuffer, 0, bytesRead)
                }
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun setup(url: String, userAgent: String): InputStream {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.useCaches = true
        connection.addRequestProperty("User-Agent", userAgent)
        connection.readTimeout = 15000
        connection.connectTimeout = 15000
        connection.doOutput = true
        return connection.inputStream
    }
}
