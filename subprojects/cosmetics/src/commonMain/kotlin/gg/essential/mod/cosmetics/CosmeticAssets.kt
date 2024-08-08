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
package gg.essential.mod.cosmetics

import gg.essential.mod.EssentialAsset
import gg.essential.model.Side
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CosmeticAssets(val allFiles: Map<String, EssentialAsset>) {
    @Transient
    private val files = object {
        val unknown = allFiles.toMutableMap()

        operator fun get(path: String): EssentialAsset? {
            unknown.remove(path)
            return allFiles[path]
        }
    }

    val thumbnail: EssentialAsset? = files["thumbnail.png"]
    val texture: EssentialAsset? = files["texture.png"]
    val emissiveTexture: EssentialAsset? = files["emissive.png"]
    val geometry: Geometry = Geometry(files["geometry.steve.json"] ?: EssentialAsset.EMPTY, files["geometry.alex.json"])
    val animations: EssentialAsset? = files["animations.json"]
    val defaultSkinMask: SkinMask = SkinMask(files["skin_mask.steve.png"], files["skin_mask.alex.png"])
    val sidedSkinMasks: Map<Side, SkinMask> = Side.values().associateWith { side ->
        val sideId = side.name.lowercase()
        SkinMask(files["skin_mask.steve.$sideId.png"], files["skin_mask.alex.$sideId.png"])
    }
    val settings: EssentialAsset? = files["settings.json"]
    val particles: Map<String, EssentialAsset> = allFiles.mapNotNull { (path, asset) ->
        if (path.startsWith("particles/") && path.endsWith(".json")) {
            files.unknown.remove(path)
            path to asset
        } else null
    }.toMap()
    val soundDefinitions: EssentialAsset? = files["sounds/sound_definitions.json"]

    val otherFiles: Map<String, EssentialAsset>
        get() = files.unknown

    @Serializable
    data class Geometry(
        val steve: EssentialAsset,
        val alex: EssentialAsset?,
    )

    @Serializable
    data class SkinMask(
        val steve: EssentialAsset?,
        val alex: EssentialAsset?,
    )
}