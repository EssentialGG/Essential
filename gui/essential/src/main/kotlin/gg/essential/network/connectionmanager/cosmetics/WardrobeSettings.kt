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

import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*

class WardrobeSettings {

    private val settings: MutableState<Settings?> = mutableStateOf(null)

    val outfitsLimit = settings.map { it?.outfitsLimit ?: 0 }
    val skinsLimit = settings.map { it?.skinsLimit ?: 0 }
    val giftingCoinSpendRequirement = settings.map { it?.giftingCoinSpendRequirement ?: 0 }
    val youNeedMinimumAmount = settings.map { it?.youNeedMinimumAmount ?: 100 }

    fun populateSettings(outfitLimit: Int, skinsLimit: Int, giftingCoinSpendRequirement: Int, youNeedMinimumAmount: Int?) {
        settings.set(Settings(outfitLimit, skinsLimit, giftingCoinSpendRequirement, youNeedMinimumAmount))
    }

    fun isSettingsLoaded(): Boolean {
        return settings.get() != null
    }

    private data class Settings(
        val outfitsLimit: Int,
        val skinsLimit: Int,
        val giftingCoinSpendRequirement: Int,
        val youNeedMinimumAmount: Int?
    )

}
