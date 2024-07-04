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
package gg.essential.gui.about.components

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CopyConstraintFloat
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.AboutMenu
import gg.essential.gui.about.Category
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.bindParent
import gg.essential.util.bindHoverEssentialTooltip
import gg.essential.util.scrollGradient
import gg.essential.vigilance.utils.onLeftClick

class RightPane(aboutMenu: AboutMenu, pages: Map<Category, Page>, selectedPage: BasicState<Page>) : UIContainer() {

    private val scroller by ScrollComponent(
        "Something went wrong. Please check your connection and try again.",
        innerPadding = 10f,
        pixelsPerScroll = 25f,
    ).constrain {
        y = 5.pixels
        width = 100.percent - 10.pixels
        height = 100.percent - 5.pixels
    } childOf this scrollGradient 20.pixels

    init {
        effect(ScissorEffect())

        val scrollContainer by UIContainer().constrain {
            y = CopyConstraintFloat() boundTo aboutMenu.content
            width = 100.percent
            height = 100.percent boundTo aboutMenu.content
        } childOf aboutMenu.rightDivider

        val scrollBar by UIBlock(EssentialPalette.SCROLLBAR).constrain {
            width = 100.percent
        } childOf scrollContainer

        scroller.setVerticalScrollBarComponent(scrollBar, true)

        // Bind the pages to display when they are selected
        pages.values.forEach { page -> page.bindParent(scroller, selectedPage.map { it == page }) }

        selectedPage.onSetValue {
            scroller.scrollToTop(smoothScroll = false)
        }

        // Scroll to top button
        val scrollToTopButton by IconButton(EssentialPalette.ARROW_UP_7X5).constrain {
            x = 0.pixels(alignOpposite = true)
            y = SiblingConstraint()
            height = AspectConstraint()
        }.onLeftClick {
            scroller.scrollToTop(true)
        }.bindHoverEssentialTooltip(BasicState("Scroll to top"), EssentialTooltip.Position.ABOVE) childOf this

        var scrollTopVisible = false

        scroller.addScrollAdjustEvent(isHorizontal = false) { scrollPercentage, _ ->
            if (scrollPercentage > 0.01 && !scrollTopVisible) {
                scrollToTopButton.animate {
                    setYAnimation(Animations.OUT_EXP, 0.5f, 0.pixels(alignOpposite = true), 0f)
                    scrollTopVisible = true
                }
            } else if (scrollPercentage <= 0.01 && scrollTopVisible) {
                scrollToTopButton.animate {
                    setYAnimation(Animations.OUT_EXP, 0.5f, SiblingConstraint(), 0f)
                    scrollTopVisible = false
                }
            }
        }
    }
}
