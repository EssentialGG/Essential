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

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import gg.essential.mod.Model
import gg.essential.mod.Skin
import java.util.*
import gg.essential.api.utils.mojang.Skin as APISkin

fun Property.propertyToSkin(): Skin {
    val skinHolder = JsonHolder(String(Base64.getDecoder().decode(this.value)))
        .optJSONObject("textures")
        .optJSONObject("SKIN")
    return Skin.fromUrl(
        url = skinHolder.optString("url"),
        model = Model.byTypeOrDefault(skinHolder.optJSONObject("metadata").optString("model")),
    )
}

fun GameProfile.gameProfileToSkin(): Skin? {
    return properties["textures"].firstOrNull()?.let {
        return it.propertyToSkin()
    }
}

fun APISkin.toMod() =
    Skin(Skin.hashFromUrl(url), Model.byVariantOrDefault(variant))
