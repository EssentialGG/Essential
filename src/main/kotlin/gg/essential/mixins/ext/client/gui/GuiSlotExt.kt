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

import gg.essential.universal.UMatrixStack
import net.minecraft.client.gui.GuiSlot

interface GuiSlotExt {
    fun `essential$setHeader`(header: GuiSlotHeader?, height: Int)
}

interface GuiSlotHeader {
    fun draw(matrixStack: UMatrixStack, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, isHovered: Boolean)
    fun mouseClick()
}

//#if MC>=11400
//$$ // TODO: this is unused, breaks because the generic uses a now protected type
//$$ // val AbstractList<*>.ext get() = this as GuiSlotExt
//#else
val GuiSlot.ext get() = this as GuiSlotExt
//#endif

// TODO this is currently unused and the corresponding mixin (MixinGuiSlot) is commented out for now
fun GuiSlotExt.setHeader(header: GuiSlotHeader?, height: Int) = `essential$setHeader`(header, height)
