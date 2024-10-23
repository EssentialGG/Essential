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
package gg.essential.network.connectionmanager.coins

import gg.essential.gui.image.EssentialAssetImageFactory
import gg.essential.mod.EssentialAsset
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.util.format
import java.util.*
import kotlin.math.pow
import kotlin.math.round

data class CoinBundle(
    val id: String,
    val numberOfCoins: Int,
    val currency: Currency,
    val price: Double,
    val extraPercent: Int,
    val iconAsset: EssentialAsset,
    val isHighlighted: Boolean,
    val isExchangeBundle: Boolean,
    val isSpecificAmount: Boolean = false,
) {

    val iconFactory = EssentialAssetImageFactory(iconAsset)

    init {
        iconFactory.primeCache(AssetLoader.Priority.Low)
    }

    val formattedPrice: String = currency.format(price)

    fun getBundleForNumberOfCoins(coins: Int): CoinBundle {
        val precision = 10.0.pow(currency.defaultFractionDigits)
        return copy(
            numberOfCoins = coins,
            price = round((price / numberOfCoins) * coins * precision) / precision,
            isSpecificAmount = true
        )
    }

}
