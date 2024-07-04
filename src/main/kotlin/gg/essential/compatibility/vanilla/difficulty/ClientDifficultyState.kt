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
// 1.12.2 and below
package gg.essential.compatibility.vanilla.difficulty

import net.minecraft.world.EnumDifficulty
import net.minecraft.world.storage.WorldInfo

interface ClientDifficultyState {
    fun `essential$setDifficultyFromServer`(difficulty: EnumDifficulty)
    fun `essential$setDifficultyLockedFromServer`(locked: Boolean)
}

private val WorldInfo.ext: ClientDifficultyState get() = this as ClientDifficultyState

fun WorldInfo.setDifficultyFromServer(difficulty: EnumDifficulty) = ext.`essential$setDifficultyFromServer`(difficulty)
fun WorldInfo.setDifficultyLockedFromServer(locked: Boolean) = ext.`essential$setDifficultyLockedFromServer`(locked)
