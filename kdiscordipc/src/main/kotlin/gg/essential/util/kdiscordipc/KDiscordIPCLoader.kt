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
package gg.essential.util.kdiscordipc

import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.socket.Socket
import dev.cbyrne.kdiscordipc.core.socket.SocketProvider
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class KDiscordIPCLoader {
    private val loader = RelaunchClassLoader(arrayOf(findExtractedBundleJar()), javaClass.classLoader).apply {
        // Socket and RawPacket are the API used for communicating with the socket implementation.
        addClassExclusion(Socket::class.java.name)
        addClassExclusion(RawPacket::class.java.name)

        // ModLauncher's findResource fails when the class is in another layer. Instead of trying to find complex
        // workarounds for that, we'll just exclude Kotlin (provided by KotlinForForge) as well.
        // As a bonus, this theoretically allows us to use Kotlin types in our API.
        addPackageExclusion("kotlin.")
        addPackageExclusion("kotlinx.")
    }

    /**
     * Returns the [Socket] implementation instance for the current OS.
     * @see dev.cbyrne.kdiscordipc.core.socket.SocketProvider
     */
    fun getPlatformSocket(): Socket {
        val providerClass = loader.loadClass(SocketProvider::class.java.name)
        val method = providerClass.getDeclaredMethod("systemDefault")
        return method.invoke(null) as Socket
    }

    companion object {
        private var extractedBundleJar: URL? = null

        private fun findExtractedBundleJar() = extractedBundleJar ?: run {
            val bundleJarResource = KDiscordIPCLoader::class.java.getResource("bundle.jar")
                ?: throw IllegalStateException("Failed to find kdiscordipc bundle jar")
            val tmpFile = Files.createTempFile("essential-kdiscordipc", ".jar")
            tmpFile.toFile().deleteOnExit()
            bundleJarResource.openStream().use {
                Files.copy(it, tmpFile, StandardCopyOption.REPLACE_EXISTING)
            }
            tmpFile.toUri().toURL().also { extractedBundleJar = it }
        }
    }
}