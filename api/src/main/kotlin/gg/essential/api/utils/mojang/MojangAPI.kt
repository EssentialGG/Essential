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
package gg.essential.api.utils.mojang

import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Utility for interacting with and getting data from the [Mojang API](https://wiki.vg/Mojang_API).
 */
interface MojangAPI {
    /**
     * Get the uuid of a player via their username ([wiki.vg](https://wiki.vg/Mojang_API#Username_to_UUID)).
     *
     * @param name player username
     * @return player's uuid
     */
    fun getUUID(name: String): CompletableFuture<UUID>?

    /**
     * Get the username of a player via their uuid ([wiki.vg](https://wiki.vg/Mojang_API#Usernames_to_UUIDs)).
     *
     * @param uuid player uuid
     * @return player's username
     */
    fun getName(uuid: UUID): CompletableFuture<String>?

    @Deprecated("Name history has been removed from the Mojang API")
    /**
     * Get the username history of a player ([wiki.vg](https://wiki.vg/Mojang_API#UUID_to_Name_History)).
     *
     * @param uuid player uuid
     * @return list of [Name] objects populated with the player's username history
     * @see Name
     */
    fun getNameHistory(uuid: UUID?): List<Name?>?

    /**
     * Get the complete player profile of a player ([wiki.vg](https://wiki.vg/Mojang_API#UUID_to_Profile_and_Skin.2FCape)).
     *
     * @param uuid player uuid
     * @return player [Profile]
     * @see Profile
     */
    fun getProfile(uuid: UUID): Profile?

    /**
     * Send a skin change request ([wiki.vg](https://wiki.vg/Mojang_API#Change_Skin)).
     * If successful, the player must leave their current server
     * and log back in to view their new skin.
     *
     * @param accessToken session token. this is used for authentication with mojang
     * @param uuid player uuid. this must match the session token
     * @param model skin will use this model (alex/steve)
     * @param url image url of the new skin
     * @return [SkinResponse] from mojang
     * @see Model
     * @see SkinResponse
     */
    fun changeSkin(accessToken: String, uuid: UUID, model: Model, url: String): SkinResponse?
}
