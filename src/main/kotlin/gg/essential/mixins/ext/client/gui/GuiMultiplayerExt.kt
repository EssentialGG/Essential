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
package gg.essential.mixins.ext.client.gui

import gg.essential.gui.multiplayer.EssentialMultiplayerGui
import gg.essential.mixins.transformers.client.gui.GuiMultiplayerAccessor
import net.minecraft.client.gui.GuiMultiplayer

interface GuiMultiplayerExt {
    fun `essential$getEssentialGui`(): EssentialMultiplayerGui
    fun `essential$refresh`()
    fun `essential$close`()
}

val GuiMultiplayer.acc get() = this as GuiMultiplayerAccessor
val GuiMultiplayer.ext get() = this as GuiMultiplayerExt
val GuiMultiplayerExt.essential get() = `essential$getEssentialGui`()
fun GuiMultiplayerExt.refresh() = `essential$refresh`()
fun GuiMultiplayerExt.close() = `essential$close`()
