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

import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.CosmeticTypeId
import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.collections.asMap
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.CosmeticType
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.network.cosmetics.Cosmetic
import java.util.concurrent.CompletableFuture

class CosmeticsDataWithChanges(
    val inner: CosmeticsData,
) : CosmeticsData {

    private val updatedCosmetics = mutableStateOf(mapOf<CosmeticId, Cosmetic?>())
    private val updatedCategories = mutableStateOf(mapOf<CosmeticCategoryId, CosmeticCategory?>())
    private val updatedTypes = mutableStateOf(mapOf<CosmeticTypeId, CosmeticType?>())
    private val updatedBundles = mutableStateOf(mapOf<CosmeticBundleId, CosmeticBundle?>())
    private val updatedFeaturedPageCollections = mutableStateOf(mapOf<FeaturedPageCollectionId, FeaturedPageCollection?>())

    override val cosmetics: ListState<Cosmetic> = stateBy {
        val originals = inner.cosmetics().associateBy { it.id }
        val updates = updatedCosmetics()
        val updatedTypes = updatedTypes()
        (originals + updates).mapNotNull { (_, cosmetic) ->
            val updatedType = cosmetic?.type?.id?.let { updatedTypes[it] }
            if (updatedType != null) {
                cosmetic.copy(type = updatedType)
            } else {
                cosmetic
            }
        }
    }.toListState()

    override val categories: ListState<CosmeticCategory> = stateBy {
        val originals = inner.categories().associateBy { it.id }
        val updates = updatedCategories()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val types: ListState<CosmeticType> = stateBy {
        val originals = inner.types().associateBy { it.id }
        val updates = updatedTypes()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val bundles: ListState<CosmeticBundle> = stateBy {
        val originals = inner.bundles().associateBy { it.id }
        val updates = updatedBundles()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val featuredPageCollections: ListState<FeaturedPageCollection> = stateBy {
        val originals = inner.featuredPageCollections().associateBy { it.id }
        val updates = updatedFeaturedPageCollections()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    private val refHolder = ReferenceHolderImpl()
    private val categoriesMap = categories.asMap(refHolder) { it.id to it }
    private val typesMap = types.asMap(refHolder) { it.id to it }
    private val bundlesMap = bundles.asMap(refHolder) { it.id to it }
    private val featuredPageCollectionsMap = featuredPageCollections.asMap(refHolder) { it.id to it }
    private val cosmeticsMap = cosmetics.asMap(refHolder) { it.id to it }

    fun updateCosmetic(id: CosmeticId, cosmetic: Cosmetic?) {
        if (inner.getCosmetic(id) != cosmetic) {
            updatedCosmetics.set { it + (id to cosmetic) }
        } else {
            updatedCosmetics.set { it - id }
        }
    }

    fun updateCategory(id: CosmeticCategoryId, category: CosmeticCategory?) {
        if (inner.getCategory(id) != category) {
            updatedCategories.set { it + (id to category) }
        } else {
            updatedCategories.set { it - id }
        }
    }

    fun updateType(id: CosmeticTypeId, type: CosmeticType?) {
        if (inner.getType(id) != type) {
            updatedTypes.set { it + (id to type) }
        } else {
            updatedTypes.set { it - id }
        }
    }

    fun updateBundle(id: CosmeticBundleId, bundle: CosmeticBundle?) {
        if (inner.getCosmeticBundle(id) != bundle) {
            updatedBundles.set { it + (id to bundle) }
        } else {
            updatedBundles.set { it - id }
        }
    }

    fun updateFeaturedPageCollection(id: FeaturedPageCollectionId, featuredPageCollection: FeaturedPageCollection?) {
        if (inner.getFeaturedPageCollection(id) != featuredPageCollection) {
            updatedFeaturedPageCollections.set { it + (id to featuredPageCollection) }
        } else {
            updatedFeaturedPageCollections.set { it - id }
        }
    }

    fun writeChangesToLocalCosmeticData(localCosmeticsData: LocalCosmeticsData): CompletableFuture<Unit> {
        return localCosmeticsData.writeChanges(
            categories = categoriesMap + updatedCategories.get().filterValues { it == null },
            types = typesMap + updatedTypes.get().filterValues { it == null },
            bundles = bundlesMap + updatedBundles.get().filterValues { it == null },
            featuredPageCollections = featuredPageCollectionsMap + updatedFeaturedPageCollections.get().filterValues { it == null },
            cosmetics = cosmeticsMap + updatedCosmetics.get().filterValues { it == null },
        )
    }

    override fun getCosmetic(id: CosmeticId): Cosmetic? = cosmeticsMap[id]
    override fun getCategory(id: CosmeticCategoryId): CosmeticCategory? = categoriesMap[id]
    override fun getType(id: CosmeticTypeId): CosmeticType? = typesMap[id]
    override fun getCosmeticBundle(id: CosmeticBundleId): CosmeticBundle? = bundlesMap[id]
    override fun getFeaturedPageCollection(id: FeaturedPageCollectionId): FeaturedPageCollection? = featuredPageCollectionsMap[id]

}
