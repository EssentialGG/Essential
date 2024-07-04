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

import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.mod.EssentialAsset
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.model.util.Instant

/**
 * Registers and returns a new [CosmeticCategory] with the supplied [id] and [displayName]
 */
fun CosmeticsDataWithChanges.registerCategory(
    id: String,
    icon: EssentialAsset,
    displayName: String,
    description: String,
    compactName: String,
    order: Int,
    tags: Set<String>,
    availableAfter: Instant?,
    availableUntil: Instant?,
) {
    if (getCategory(id) != null) {
        throw IllegalArgumentException("A category with the ID $id already exists")
    }
    updateCategory(
        id,
        CosmeticCategory(
            id,
            icon,
            mapOf("en_us" to displayName),
            mapOf("en_us" to description),
            mapOf("en_us" to compactName),
            setOf(),
            tags,
            order,
            availableAfter,
            availableUntil
        )
    )
}

/**
 * Unregisters the [CosmeticCategory] with the supplied [categoryId]. If any cosmetics are still using
 * this category, an [IllegalArgumentException] will be thrown
 */
fun CosmeticsDataWithChanges.unregisterCategory(categoryId: CosmeticCategoryId) {
    // Remove the category from all cosmetics that are in it
    if (cosmetics.get().any { cosmetic ->
            categoryId in cosmetic.categories.keys
        }) {
        throw IllegalArgumentException("Cannot unregister a category that is in use by a cosmetic")
    }

    updateCategory(categoryId, null)
}

/**
 * Resets a category to its default values
 */
fun CosmeticsDataWithChanges.resetCategory(categoryId: CosmeticCategoryId) {
    updateCategory(categoryId, inner.getCategory(categoryId))
}
