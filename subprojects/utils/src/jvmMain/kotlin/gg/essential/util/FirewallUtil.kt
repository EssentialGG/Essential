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

import org.slf4j.LoggerFactory
import java.io.IOException

object FirewallUtil {
    private val LOGGER = LoggerFactory.getLogger(FirewallUtil::class.java)

    /**
     * Checks if the (currently only macOS) firewall is blocking incoming connections
     */
    fun isFirewallBlocking(): Boolean {
        if (os == OperatingSystem.MACOS) {
            val builder = ProcessBuilder("/usr/libexec/ApplicationFirewall/socketfilterfw", "--getglobalstate")
            builder.redirectErrorStream(true)
            try {
                val process = builder.start()
                process.inputStream.bufferedReader().use { reader ->
                    if (reader.readLine().contains("State = 2")) {
                        LOGGER.warn("macOS Firewall is blocking connections")
                        return true
                    }
                }
                process.waitFor()
            } catch (exception: IOException) {
                LOGGER.error("Error checking macOS firewall status", exception)
            } catch (exception: InterruptedException) {
                LOGGER.error("Error checking macOS firewall status", exception)
            }
        }
        return false
    }
}
