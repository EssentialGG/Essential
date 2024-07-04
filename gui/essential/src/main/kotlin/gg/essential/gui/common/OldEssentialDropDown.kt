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

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.util.hoveredState
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick

class OldEssentialDropDown(
    initialSelection: Int,
    private val options: List<String>,
    private val maxDisplayOptions: Int = 6,
) : UIBlock() {

    private val writableExpandedState: State<Boolean> = BasicState(false)
    private val dropdownAnimating = BasicState(false)
    private var animationCounter = 0

    /** Public States **/
    val selectedIndex: State<Int> = BasicState(initialSelection)
    val selectedText: State<String> = selectedIndex.map {
        options[it]
    }
    val expandedState = ReadOnlyState(writableExpandedState)

    private val selectedArea by UIContainer().constrain {
        width = 100.percent
        height = 17.pixels
    } childOf this

    private val selectAreaHovered = selectedArea.hoveredState()

    private val currentSelectionText by EssentialUIText(shadowColor = EssentialPalette.TEXT_SHADOW).bindText(selectedText).constrain {
        x = 5.pixels
        y = CenterConstraint()
        color = EssentialPalette.getTextColor(selectAreaHovered).toConstraint()
    } childOf selectedArea

    private val iconState = writableExpandedState.map {
        if (it) {
            EssentialPalette.ARROW_UP_7X4
        } else {
            EssentialPalette.ARROW_DOWN_7X4
        }
    }

    private val downArrow by ShadowIcon(iconState, BasicState(true)).constrain {
        x = 5.pixels(alignOpposite = true)
        y = CenterConstraint()
    }.rebindPrimaryColor(EssentialPalette.getTextColor(selectAreaHovered)).rebindShadowColor(BasicState(EssentialPalette.COMPONENT_BACKGROUND)) childOf selectedArea


    private val expandedBlock by UIBlock(EssentialPalette.BUTTON_HIGHLIGHT).constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = 0.pixels // Start collapsed
    }.bindFloating(writableExpandedState or dropdownAnimating) childOf this effect ScissorEffect()

    private val scrollerContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = 100.percent - 4.pixels
        height = (100.percent - 4.pixels).coerceAtLeast(0.pixels)
    } childOf expandedBlock

    private val scroller by ScrollComponent().centered().constrain {
        width = 100.percent
        height = 100.percent
    } childOf scrollerContainer

    private val expandedContentArea by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        x = CenterConstraint()
        width = 100.percent
        height = ChildBasedSizeConstraint() + 6.pixels
    } childOf scroller

    private val expandedContent by UIContainer().centered().constrain {
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf expandedContentArea

    private val scrollbarContainer by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = CenterConstraint()
        width = 2.pixels
        height = 100.percent
    }.onLeftClick {
        it.stopPropagation()
    } childOf scrollerContainer

    private val scrollbar by UIBlock(EssentialPalette.SCROLLBAR).constrain {
        width = 100.percent
    } childOf scrollbarContainer

    private fun getMaxItemWidth(): Float {
        return options.maxOf {
            it.width()
        }
    }

    private val scrollable = options.size > maxDisplayOptions

    /**
     * @return The default width of this dropdown based on its contents
     */
    fun getDefaultWidth(): Float {
        return getMaxItemWidth() + 25
    }

    init {
        constrain {
            width = (getDefaultWidth()).pixels
            height = ChildBasedSizeConstraint()
        }

        setColor((selectAreaHovered or expandedState).map {
            if (it) {
                EssentialPalette.BUTTON_HIGHLIGHT
            } else {
                EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT
            }
        }.toConstraint())

        if (scrollable) {
            scroller.setVerticalScrollBarComponent(scrollbar, hideWhenUseless = false)
        }

        options.withIndex().forEach { (index, value) ->
            val optionContainer by UIBlock().constrain {
                y = SiblingConstraint()
                width = 100.percent
                height = 20.pixels
            }.onLeftClick {
                USound.playButtonPress()
                it.stopPropagation()
                select(index)
            } childOf expandedContent
            val hovered = optionContainer.hoveredState()

            optionContainer.setColor(hovered.map {
                if (it) {
                    EssentialPalette.BUTTON
                } else {
                    EssentialPalette.COMPONENT_BACKGROUND
                }
            }.toConstraint())
            val text by EssentialUIText(value, shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT).constrain {
                x = 5.pixels
                y = CenterConstraint()
                color = EssentialPalette.getTextColor(hovered).toConstraint()
            } childOf optionContainer
        }

        selectedArea.onLeftClick { event ->
            USound.playButtonPress()
            event.stopPropagation()

            if (writableExpandedState.get()) {
                collapse()
            } else {
                expand()
            }
        }
    }

    fun select(index: Int) {
        if (index in options.indices) {
            selectedIndex.set(index)
            collapse()
        }
    }

    fun expand(instantly: Boolean = false) {
        writableExpandedState.set(true)
        applyExpandedBlockHeight(
            instantly,
            (options.size.coerceAtMost(maxDisplayOptions) * 20).pixels + 10.pixels
        )
    }

    fun collapse(instantly: Boolean = false) {
        writableExpandedState.set(false)
        applyExpandedBlockHeight(instantly, 0.pixels)
    }

    private fun applyExpandedBlockHeight(
        instantly: Boolean,
        heightConstraint: HeightConstraint,
    ) {
        if (instantly) {
            expandedBlock.setHeight(heightConstraint)
            dropdownAnimating.set(false)
        } else {
            val counterInstance = ++animationCounter
            dropdownAnimating.set(true)
            expandedBlock.animate {
                setHeightAnimation(Animations.OUT_EXP, 0.25f, heightConstraint)
                onComplete {
                    if (counterInstance == animationCounter) {
                        dropdownAnimating.set(false)
                    }
                }
            }
        }
    }
}
