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
package gg.essential.gui.common.modal

import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.*

/** A [ConfirmDenyModal] with a searchbar and scroll container */
open class SearchableConfirmDenyModal(
    modalManager: ModalManager,
    requiresButtonPress: Boolean,
    searchbarPadding: Float = 13f,
) : ConfirmDenyModal(modalManager, requiresButtonPress) {

    private val searchContainer by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf customContent

    val searchbar by EssentialSearchbar() childOf searchContainer

    protected val middleSpacer by Spacer(height = searchbarPadding) childOf searchContainer

    protected val scrollContainer by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedMaxSizeConstraint()
    } effect ScissorEffect() childOf customContent

    val scroller by ScrollComponent(emptyString = "Nothing Found").constrain {
        width = FillConstraint()
        height = 148.pixels
    } childOf scrollContainer scrollGradient 30.pixels

    val scrollbarContainer by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = 0.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint() * 2
        height = 100.percent boundTo scroller
    } childOf scrollContainer

    private val scrollBarBackground by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        x = 0.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint()
        height = 100.percent
    } childOf scrollbarContainer

    private val scrollBar by UIBlock(EssentialPalette.SCROLLBAR).setWidth(2.pixels) childOf scrollBarBackground

    private val hiddenSpacer by Spacer().constrain {
        height = 100.percent boundTo searchContainer
    } hiddenChildOf customContent

    protected val bottomSpacer by Spacer(height = 8f) childOf customContent

    init {
        configure {
            titleTextColor = EssentialPalette.TEXT_HIGHLIGHT
        }

        scroller.emptyText.constrain {
            y += 2.pixels
            color = EssentialPalette.TEXT_DISABLED.toConstraint()
        }.setShadowColor(EssentialPalette.COMPONENT_BACKGROUND)
        scroller.setVerticalScrollBarComponent(scrollBar, true)

        spacer.setHeight(10.pixels)
    }

    /**
     * Hides the searchbar instantly
     */
    fun hideSearchbar() {
        searchContainer.hide(true)
        hiddenSpacer.unhide()
    }

    /**
     * Shows the searchbar
     */
    fun showSearchbar() {
        searchContainer.unhide()
        hiddenSpacer.hide(true)
    }
}
