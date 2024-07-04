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
package gg.essential.gui.elementa.effects

import gg.essential.elementa.UIComponent
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.UMatrixStack

class ZIndexEffect(val index: Int, val parent: UIComponent? = null) : Effect() {
    private val scissor = ScissorEffect(0, 0, 0, 0, false)
    private var passThrough: Boolean = false

    override fun beforeDraw(matrixStack: UMatrixStack) {
        if (!passThrough) {
            scissor.beforeDraw(matrixStack)
        }
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        if (!passThrough) {
            scissor.afterDraw(matrixStack)

            val parent = parent ?: boundComponent.parent
            val coordinator = parent.effects.firstNotNullOfOrNull { it as? Coordinator }
                ?: Coordinator().also { parent.enableEffect(it) }
            coordinator.toBeDrawn.add(this)
        }
    }

    private class Coordinator : Effect() {
        val toBeDrawn = mutableListOf<ZIndexEffect>()

        override fun afterDraw(matrixStack: UMatrixStack) {
            toBeDrawn.sortBy { it.index }
            toBeDrawn.forEach {  effect ->
                effect.passThrough = true
                effect.boundComponent.draw(matrixStack)
                effect.passThrough = false
            }
            toBeDrawn.clear()

            super.afterDraw(matrixStack)
        }
    }
}