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
package gg.essential.mod.cosmetics

import gg.essential.mod.Model
import gg.essential.mod.Skin

data class OutfitSkin(
    val skin: Skin,
    val locked: Boolean,
) {
    constructor(
        hash: String,
        model: Model,
        locked: Boolean,
    ) : this(Skin(hash, model), locked)

    fun serialize(): String {
        val (hash, model) = skin
        val modelStr = model.ordinal
        val lockedStr = if (locked) "1" else "0"
        return "$modelStr;$hash;$lockedStr"
    }

    companion object {
        @JvmStatic
        fun deserialize(string: String?): OutfitSkin? {
            val parts = (string ?: return null)
                .split(";")
                .toTypedArray()
            if (parts.size < 2) return null
            return OutfitSkin(
                model = if (parts[0] == "1") Model.ALEX else Model.STEVE,
                hash = parts[1],
                locked = parts.size >= 3 && parts[2] == "1",
            )
        }
    }
}
