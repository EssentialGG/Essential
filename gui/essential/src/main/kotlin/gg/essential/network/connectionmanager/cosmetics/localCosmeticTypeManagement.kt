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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.cosmetics.CosmeticTypeId
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.CosmeticType

/**
 * Registers and returns a new [CosmeticType] with the supplied [id], [name], and [slot]
 */
fun CosmeticsDataWithChanges.registerType(id: String, name: String, slot: CosmeticSlot) {
    if (types.get().any { it.id == id }) {
        throw IllegalArgumentException("A type with id $id already exists")
    }

    val type = CosmeticType(id, slot, mapOf("en_us" to name), mutableMapOf()) // skinLayers seems to be unused and absent from git based cosmetic

    updateType(type.id, type)
}

/**
 * Resets a type to its default values
 */
fun CosmeticsDataWithChanges.resetType(typeId: CosmeticTypeId) {
    updateType(typeId, inner.getType(typeId))
}
