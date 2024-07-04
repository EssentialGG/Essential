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

import com.google.common.net.HostAndPort
import com.google.common.net.InetAddresses

object AddressUtil {

    @JvmStatic
    fun removeDefaultPort(address: String): String {
        return address.removeSuffix(":25565")
    }

    @JvmStatic
    fun isLanOrLocalAddress(address: String): Boolean {
        val host = HostAndPort.fromString(address).host

        if (host.equals("localhost", ignoreCase = true)) {
            return true
        }
        try {
            // Guava INetAddresses avoids DNS lookups
            InetAddresses.forString(host).let { inetAddress ->
                return inetAddress.isSiteLocalAddress || inetAddress.isLoopbackAddress
            }
        } catch (ignored: IllegalArgumentException) { }
        return false
    }

    @JvmStatic
    fun isSpecialFormattedAddress(address: String): Boolean {
        return address == SINGLEPLAYER || address == LOCAL_SERVER
    }

    // These constants are specifically whitelisted on the connection manager's handler for ClientProfileActivityPacket.
    // If any further ones are added, they must be added to the whitelist, or they will be stripped from the activity packet.
    const val SINGLEPLAYER = "Singleplayer"

    const val LOCAL_SERVER = "Local Server"
}