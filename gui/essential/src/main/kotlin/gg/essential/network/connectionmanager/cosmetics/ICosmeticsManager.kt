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

import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.model.CosmeticUnlockData
import gg.essential.gui.elementa.state.v2.State
import java.util.concurrent.CompletableFuture

interface ICosmeticsManager {
    val cosmeticsData: CosmeticsData
    val infraCosmeticsData: InfraCosmeticsData
    val localCosmeticsData: LocalCosmeticsData?
    val cosmeticsDataWithChanges: CosmeticsDataWithChanges?

    val unlockedCosmetics: State<Set<CosmeticId>>
    val unlockedCosmeticsData: State<Map<CosmeticId, CosmeticUnlockData>>

    fun unlockAllCosmetics()
    fun clearUnlockedCosmetics()

    // FIXME inline? coroutines?
    fun claimFreeItems(ids: Set<String>): CompletableFuture<Boolean>
}
