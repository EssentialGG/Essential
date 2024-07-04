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
package gg.essential.handlers.screenshot

import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

object ScreenshotUploadUtil {

    private const val CRLF = "\r\n"

    @Throws(MalformedURLException::class, IOException::class)
    fun httpUpload(url: String, fileData: ByteArray): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        val boundary = "---------------" + System.currentTimeMillis().toString(16)
        connection.doOutput = true
        connection.doInput = true
        connection.useCaches = true
        connection.requestMethod = "POST"
        connection.addRequestProperty("User-Agent", "Essential")
        connection.setRequestProperty("Connection", "Keep-Alive")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        val directOutput = connection.outputStream
        val body = PrintWriter(directOutput.writer(), true)
        body.append(CRLF)
        addFileData(fileData, body, directOutput, boundary)
        addCloseDelimiter(body, boundary)
        return (connection.responseCode / 100) == 2
    }


    @Throws(IOException::class)
    private fun addFileData(byteStream: ByteArray, body: PrintWriter, directOutput: OutputStream, boundary: String) {
        body.append("--").append(boundary).append(CRLF)
        body.append("""Content-Disposition: form-data; name="file"; filename="file"""").append(CRLF)
        body.append("Content-Type: image/png").append(CRLF)
        body.append("Content-Transfer-Encoding: binary").append(CRLF)
        body.append(CRLF)
        body.flush()
        directOutput.write(byteStream)
        directOutput.flush()
        body.append(CRLF)
        body.flush()
    }

    private fun addCloseDelimiter(body: PrintWriter, boundary: String) {
        body.append("--").append(boundary).append("--").append(CRLF)
        body.flush()
    }
}