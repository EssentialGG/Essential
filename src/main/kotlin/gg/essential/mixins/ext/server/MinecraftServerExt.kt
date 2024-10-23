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
package gg.essential.mixins.ext.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import net.minecraft.server.MinecraftServer

interface MinecraftServerExt {
    val `essential$dispatcher`: CoroutineDispatcher
    val `essential$coroutineScope`: CoroutineScope
}

val MinecraftServer.ext get() = this as MinecraftServerExt
val MinecraftServer.dispatcher get() = ext.`essential$dispatcher`
val MinecraftServer.coroutineScope get() = ext.`essential$coroutineScope`
