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
package gg.essential.api.profile

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.PropertyMap
import java.util.*

/**
 * A wrapper for [GameProfile] which correctly implements [equals] and [hashCode].
 */
class WrappedGameProfile(
    val profile: GameProfile
) {
    constructor(id: UUID, name: String) : this(GameProfile(id, name))

    val id: UUID
        get() = profile.id ?: UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray())

    val name: String
        get() = profile.name ?: ""

    val properties: PropertyMap
        get() = profile.properties

    fun copy(): WrappedGameProfile {
        val profile = GameProfile(profile.id, profile.name)
        profile.properties.putAll(properties)

        return WrappedGameProfile(profile)
    }

    override fun hashCode(): Int {
        var result = profile.id?.hashCode() ?: 0
        result = 31 * result + (profile.name?.hashCode() ?: 0)
        result = 31 * result + profile.properties.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WrappedGameProfile) return false

        if (profile.id != other.profile.id) return false
        if (profile.name != other.profile.name) return false
        if (profile.properties != other.profile.properties) return false

        return true
    }
}

/**
 * A simple extension function for wrapping a [GameProfile].
 * The constructor is fine to use too, but this can help make null-chaining profile conversions more readable.
 */
fun GameProfile.wrapped() = WrappedGameProfile(this)