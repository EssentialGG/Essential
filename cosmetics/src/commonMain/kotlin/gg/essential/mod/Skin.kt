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
package gg.essential.mod

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Skin(
    val hash: String,
    val model: Model,
) {

    val url = String.format(Locale.ROOT, SKIN_URL, hash)

    companion object {

        // EM-2483: Minecraft uses http for skin textures and using https causes issues due to missing CA certs on outdated java versions.
        @Suppress("HttpUrlsUsage")
        const val SKIN_URL = "http://textures.minecraft.net/texture/%s"

        @JvmStatic
        fun fromUrl(url: String, model: Model) =
            Skin(hashFromUrl(url), model)

        @JvmStatic
        fun hashFromUrl(url: String): String =
            url.split("/").lastOrNull() ?: ""

    }
}
