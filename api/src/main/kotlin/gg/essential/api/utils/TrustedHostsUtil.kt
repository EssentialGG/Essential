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

/**
 * Utility for interacting with Essential's trusted image host list.
 */
interface TrustedHostsUtil {
    /**
     * Get all hosts from the TrustedHosts list as [TrustedHost]s.
     *
     * @return hosts from the TrustedHosts list.
     * @see TrustedHost
     */
    fun getTrustedHosts(): Set<TrustedHost>

    /**
     * Get a [TrustedHost] object from the TrustedHosts list using
     * it's ID.
     *
     * @return host with the specified id (or null if none with the id is found)
     * @see TrustedHost
     */
    fun getTrustedHostByID(id: String): TrustedHost?

    /**
     * Add an image host to the TrustedHosts list.
     *
     * @param host host to be added
     */
    fun addTrustedHost(host: TrustedHost)

    /**
     * Remove an image host from the TrustedHosts list.
     *
     * @param hostId ID of the host to be removed.
     */
    fun removeTrustedHost(hostId: String)

    data class TrustedHost(val id: String, val name: String, val domains: Set<String>)
}
