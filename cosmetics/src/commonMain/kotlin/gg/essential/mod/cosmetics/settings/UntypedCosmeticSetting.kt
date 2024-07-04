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
package gg.essential.mod.cosmetics.settings

import gg.essential.model.util.AnySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UntypedCosmeticSetting(
    val id: String?,
    val type: String,
    @SerialName("enabled")
    val isEnabled: Boolean,
    val data: Map<String, @Serializable(with = AnySerializer::class) Any>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String): T? {
        return data[key] as T?
    }

    fun hasData(key: String): Boolean {
        return data.containsKey(key)
    }
}
