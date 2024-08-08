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
package gg.essential.mod.cosmetics.featured

import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticId
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed class FeaturedItem {

    abstract val width: Int
    abstract val height: Int
    abstract val type: FeaturedItemType

    @SerialName("COSMETIC")
    @Serializable
    data class Cosmetic(
        val cosmetic: CosmeticId,
        val settings: List<CosmeticSetting>,
        override val width: Int = 1,
        override val height: Int = width,
    ) : FeaturedItem() {

        @Transient
        override val type: FeaturedItemType = FeaturedItemType.COSMETIC

    }

    @SerialName("BUNDLE")
    @Serializable
    data class Bundle(
        val bundle: CosmeticBundleId,
        override val width: Int = 2,
        override val height: Int = width,
    ) : FeaturedItem() {

        @Transient
        override val type: FeaturedItemType = FeaturedItemType.BUNDLE

    }

    @SerialName("EMPTY")
    @Serializable
    data class Empty(
        override val width: Int,
        override val height: Int = width,
    ) : FeaturedItem() {

        @Transient
        override val type: FeaturedItemType = FeaturedItemType.EMPTY

    }

}


