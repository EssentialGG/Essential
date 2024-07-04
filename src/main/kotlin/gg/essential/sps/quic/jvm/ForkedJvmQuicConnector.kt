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
package gg.essential.sps.quic.jvm

import gg.essential.util.ForkedJvm
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/**
 * A QUIC connector running in a separate JVM (because the Netty in the current one may be incompatible with what we
 * need).
 */
class ForkedJvmQuicConnector(main: String) : Closeable {
    // Force language to english to workaround https://github.com/bcgit/bc-java/issues/879. Netty SelfSignedCertificate
    // uses Bouncy Castle to generate the certificate.
    private val jvm = ForkedJvm(main, quicConnectorClasspath(), listOf("-Duser.language=en"))
    val output = DataOutputStream(jvm.process.outputStream)
    val input = DataInputStream(jvm.process.inputStream)

    @Synchronized
    override fun close() {
        // Prompt the connector to quit once it is quiet
        try {
            output.write(-1)
            output.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Wait for it to quit (the EventLoopGroup itself has a 15 seconds timeout, so we'll wait at most 20)
        try {
            jvm.process.waitFor(20, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.interrupted()
        }

        // Enough time has passed, kill it
        jvm.close()
    }

    companion object {
        private val extractedNettyJar: Path by lazy {
            val nettyJarResource = ForkedJvmQuicConnector::class.java.getResource("netty.jar")
                ?: throw IllegalStateException("Failed to find quic connector netty jar")
            val tmpFile = Files.createTempFile("essential-quic-connector", ".jar")
            tmpFile.toFile().deleteOnExit()
            nettyJarResource.openStream().use {
                Files.copy(it, tmpFile, StandardCopyOption.REPLACE_EXISTING)
            }
            tmpFile.toAbsolutePath()
        }

        private fun quicConnectorClasspath(): String {
            return extractedNettyJar.toString() + System.getProperty("path.separator") + ForkedJvm.defaultClassPath()
        }
    }
}