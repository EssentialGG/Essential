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

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Name history entry.
 *
 * @param name username
 * @param changedToAt timestamp of when the name was changed
 */
data class Name(val name: String?, val changedToAt: Long?)

/**
 * Player profile ([wiki.vg](https://wiki.vg/Mojang_API#UUID_to_Profile_and_Skin.2FCape)).
 *
 * @param id player uuid (no dashes, as string)
 * @param name player username
 * @param properties profile properties
 */
data class Profile(val id: String?, val name: String?, val properties: List<Property>?) {
    /**
     * Skin and, if present, cape.
     */
    val textures: ProfileTextures
        get() {
            val decoded = String(Base64.getDecoder().decode(properties?.get(0)?.value))
            return ProfileTextures.fromJson(decoded)!!
        }
}

/**
 * @param profileId player uuid
 * @param profileName player name
 * @param textures skin and cape texture urls
 */
data class ProfileTextures(
    val timestamp: Long?,
    val profileId: String?,
    val profileName: String?,
    val textures: Textures?
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): ProfileTextures? =
            Gson().fromJson(json, ProfileTextures::class.java)
    }
}

/**
 * @param skin url to player's skin
 * @param cape url to player's cape
 */
data class Textures(@SerializedName("SKIN") val skin: TextureURL?, @SerializedName("CAPE") val cape: TextureURL?)

data class TextureURL(val url: String?)

data class Property(val name: String?, val value: String?)

/**
 * Skin model.
 */
enum class Model(
    @Deprecated("This is probably not what you are looking for.")
    val model: String,
    /** The internal name used by Minecraft to refer to this Model. */
    val type: String,
    /** The name used by Mojang services to refer to this Model (case insensitive). */
    val variant: String,
) {
    STEVE("", "default", "classic"),
    ALEX("slim", "slim", "slim");

    companion object {
        @JvmStatic
        fun byType(str: String) = values().find { it.type == str }

        @JvmStatic
        fun byTypeOrDefault(str: String) = byType(str) ?: STEVE

        @JvmStatic
        fun byVariant(str: String) = values().find { it.variant.equals(str, ignoreCase = true) }

        @JvmStatic
        fun byVariantOrDefault(str: String) = byVariant(str) ?: STEVE
    }
}

data class Skin(val id: String, val state: String, val url: String, val variant: String)

data class SkinResponse(val id: String, val name: String, val skins: List<Skin>?, val capes: List<Skin>)
