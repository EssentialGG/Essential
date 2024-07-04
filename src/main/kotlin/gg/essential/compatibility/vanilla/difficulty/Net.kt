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

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

object Net {
    @JvmField
    val WRAPPER: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("essential|diffct")

    fun init() {
        var id = 0
        with(WRAPPER) {
            registerMessage(UpdateDifficulty.Handler(), UpdateDifficulty::class.java, id++, Side.SERVER)
            registerMessage(UpdateDifficulty.Handler(), UpdateDifficulty::class.java, id++, Side.CLIENT)
            registerMessage(UpdateDifficultyLock.Handler(), UpdateDifficultyLock::class.java, id++, Side.SERVER)
            registerMessage(UpdateDifficultyLock.Handler(), UpdateDifficultyLock::class.java, id++, Side.CLIENT)
        }
    }
}