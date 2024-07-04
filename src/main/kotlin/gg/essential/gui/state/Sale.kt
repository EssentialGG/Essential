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
package gg.essential.gui.state

import gg.essential.cosmetics.CosmeticId
import gg.essential.network.cosmetics.Cosmetic
import java.time.Instant

data class Sale(
    val expiration: Instant,
    val name: String,
    val compactName: String?,
    val discountPercent: Int,
    val displayRemainingTimeOnBanner: Boolean,
    val category: String?,
    val onlyPackages: Set<Int>?,
    val onlyCosmetics: Set<CosmeticId>?,
    val tooltip: String?,
    val couponCode: String?, // Sent by infra for the new customer promotion
) {
    operator fun contains(cosmetic: Cosmetic): Boolean {
        return onlyCosmetics == null || cosmetic.id in onlyCosmetics
    }
}