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
package gg.essential.gui.common

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.universal.USound
import gg.essential.util.bindEssentialTooltip
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.round

abstract class EssentialSlider(
    initialValueFraction: Float
) : UIContainer() {

    private val notchWidth = 3

    val fraction = BasicState(initialValueFraction)
    private val updates = mutableListOf<(Float) -> Unit>()

    private val sliderBar by UIBlock(EssentialPalette.BUTTON_HIGHLIGHT).constrain {
        width = 100.percent
        height = 100.percent - 2.pixels
        y = CenterConstraint()
    } childOf this

    private val sliderNotch by UIBlock().constrain {
        width = notchWidth.pixels
        height = 100.percent
        y = CenterConstraint()
        x = basicXConstraint {
            it.parent.getLeft() + fraction.get() * (it.parent.getWidth() - notchWidth)
        }
    } childOf this

    private val sliderCovered by UIBlock(EssentialPalette.ACCENT_BLUE).constrain {
        height = 100.percent - 2.pixels
        width = basicWidthConstraint {
            sliderNotch.getLeft() - this@EssentialSlider.getLeft()
        }
        y = CenterConstraint()
    } childOf this

    private var hoveredState: State<Boolean>

    init {
        // Elementa's onMouseDrag does not check whether the mouse is within the component
        // So we need to do that ourselves. We want to ignore any drag that does not start within
        // this component
        val mouseHeld = BasicState(false)

        onLeftClick {
            USound.playButtonPress()
            mouseHeld.set(true)
            updateSlider(it.absoluteX - this@EssentialSlider.getLeft())
            it.stopPropagation()
        }
        onMouseRelease {
            mouseHeld.set(false)
        }
        sliderBar.onMouseDrag { mouseX, _, _ ->

            if (mouseHeld.get()) {
                updateSlider(mouseX)
            }
        }
        hoveredState = hoveredState() or sliderNotch.hoveredState() or mouseHeld

        sliderNotch.setColor(EssentialPalette.getTextColor(hoveredState).toConstraint())
    }

    override fun afterInitialization() {
        super.afterInitialization()
        // Kotlin's properties set in a constructor don't have their values written to
        // until after the parent object is initialized. If this was in the constructor,
        // the result of reduceFractionToDisplay would not yield the expected result
        sliderNotch.bindEssentialTooltip(hoveredState, fraction.map { fraction ->
            reduceFractionToDisplay(fraction)
        })
    }

    /**
     * Updates the slider based on the mouseX position
     */
    private fun updateSlider(mouseX: Float) {
        val updatedValue = updateSliderValue(
            ((mouseX - sliderNotch.getWidth() / 2) / (getWidth() - sliderNotch.getWidth())).coerceIn(0f..1f)
        )
        fraction.set(updatedValue)
    }

    abstract fun reduceFractionToDisplay(fraction: Float): String

    /**
     * Allows overriding of notch position of the slider to snap to desired values
     */
    open fun updateSliderValue(fraction: Float): Float {
        return fraction
    }
}

class IntEssentialSlider(
    private val minValue: Int,
    private val maxValue: Int,
    initialValue: Int
) : EssentialSlider(
    (initialValue - minValue) / (maxValue - minValue).toFloat()
) {

    private val updates = mutableListOf<(Int) -> Unit>()

    private val intValue = fraction.map {
        mapFractionToRange(it)
    }

    private fun mapFractionToRange(fraction: Float): Int {
        val range = maxValue - minValue
        return (minValue + round(fraction * range)).toInt().coerceIn(minValue..maxValue)
    }

    init {
        intValue.onSetValue {
            for (update in updates) {
                update(it)
            }
        }
    }

    fun onUpdateInt(callback: (Int) -> Unit) {
        updates.add(callback)
    }

    override fun reduceFractionToDisplay(fraction: Float): String {
        return mapFractionToRange(fraction).toString()
    }

    override fun updateSliderValue(fraction: Float): Float {
        val range = maxValue - minValue
        return round(fraction * range) / range
    }
}


