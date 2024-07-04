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

import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.FeaturedPageWidth
import gg.essential.mod.cosmetics.featured.FeaturedPage
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection

fun CosmeticsDataWithChanges.registerFeaturedPageCollection(
    id: FeaturedPageCollectionId,
    availability: FeaturedPageCollection.Availability?,
    pages: Map<FeaturedPageWidth, FeaturedPage>
) {
    if (getFeaturedPageCollection(id) != null) {
        throw IllegalArgumentException("A featured page collection with the ID $id already exists")
    }
    updateFeaturedPageCollection(
        id,
        FeaturedPageCollection(
            id,
            availability,
            pages
        )
    )
}

fun CosmeticsDataWithChanges.resetFeaturedPageCollection(id: FeaturedPageCollectionId) {
    updateFeaturedPageCollection(id, inner.getFeaturedPageCollection(id))
}

