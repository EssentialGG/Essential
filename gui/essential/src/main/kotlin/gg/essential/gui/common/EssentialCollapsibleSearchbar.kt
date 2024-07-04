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
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.input.UITextInput
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.universal.UKeyboard
import gg.essential.gui.util.isInComponentTree
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

open class EssentialSearchbar(
    placeholder: String = "Search...",
    placeholderColor: Color = EssentialPalette.TEXT,
    initialValue: String = "",
    private val activateOnSearchHokey: Boolean = true,
    private val activateOnType: Boolean = true,
    private val clearSearchOnEscape: Boolean = false,
) : UIContainer() {
    val textContentV2 = mutableStateOf(initialValue)
    val textContent = textContentV2.toV1(this)

    protected val searchContainer by UIBlock(EssentialPalette.BUTTON).constrain {
        width = 100.percent
        height = 100.percent
    } childOf this

    private val searchIcon by ShadowIcon(EssentialPalette.SEARCH_7X, true).constrain {
        x = 5.pixels
        y = CenterConstraint()
    }.rebindPrimaryColor(placeholderColor.state()) childOf searchContainer

    protected val searchInput: UITextInput by UITextInput(placeholder = placeholder, shadowColor = EssentialPalette.BLACK).constrain {
        x = SiblingConstraint(5f)
        y = CenterConstraint()
        width = FillConstraint(useSiblings = false)
    } childOf searchContainer

    init {
        constrain {
            width = 100.percent
            height = 17.pixels
        }

        searchContainer.onLeftClick {
            activateSearch()
        }

        searchInput.onKeyType { _, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE && clearSearchOnEscape) {
                textContentV2.set("")
            }
        }

        searchInput.placeholderColor.set(placeholderColor)

        searchInput.onUpdate {
            textContentV2.set(it)
        }
        textContentV2.onSetValue(this) {
            if (it != searchInput.getText()) {
                searchInput.setText(it)
            }
        }

        effect(ShadowEffect(Color.BLACK))
    }

    override fun afterInitialization() {
        super.afterInitialization()

        Window.of(this).onKeyType { typedChar, keyCode ->
            if (!this@EssentialSearchbar.isInComponentTree()) {
                return@onKeyType
            }

            when {
                activateOnSearchHokey && keyCode == UKeyboard.KEY_F && UKeyboard.isCtrlKeyDown()
                        && !UKeyboard.isShiftKeyDown() && !UKeyboard.isAltKeyDown() -> {
                    activateSearch()
                }
                activateOnType && !typedChar.isISOControl() -> {
                    searchInput.setActive(true)
                    searchInput.keyType(typedChar, keyCode)
                    searchInput.setActive(false)
                    activateSearch()
                }
            }
        }
    }

    fun setText(text: String) {
        searchInput.setText(text)
        textContent.set(text)
    }

    fun getText(): String {
        return textContent.get()
    }

    fun activateSearch() {
        searchInput.grabWindowFocus()
    }

}

class EssentialCollapsibleSearchbar(
    placeholder: String = "Search...",
    placeholderColor: Color = EssentialPalette.TEXT,
    initialValue: String = "",
    activateOnSearchHokey: Boolean = true,
    activateOnType: Boolean = true,
    expandedWidth: Int = 95,
) : EssentialSearchbar(
    placeholder,
    placeholderColor,
    initialValue,
    activateOnSearchHokey,
    activateOnType,
) {

    private val collapsed = BasicState(true)

    private val toggleIcon = collapsed.map {
        if (it) {
            EssentialPalette.SEARCH_7X
        } else {
            EssentialPalette.CANCEL_5X
        }
    }

    private val toggleButton by IconButton(
        toggleIcon,
        tooltipText = "".state(),
        enabled = true.state(),
        buttonText = "".state(),
        iconShadow = true.state(),
        textShadow = true.state(),
        tooltipBelowComponent = true,
    ).constrain {
        x = 0.pixels(alignOpposite = true)
        width = AspectConstraint()
        height = 100.percent
    }.onLeftClick {
        collapsed.set { !it }
        if (collapsed.get()) {
            textContent.set("")
        } else {
            activateSearch()
        }
    } childOf this

    init {
        constrain {
            width = ChildBasedSizeConstraint()
        }

        searchContainer.setWidth(expandedWidth.pixels).bindParent(this, !collapsed)

        // Expand on ctrl+f or type
        searchInput.onFocus { expand() }
    }

    fun collapse() {
        collapsed.set(true)
    }

    fun expand() {
        collapsed.set(false)
    }
}