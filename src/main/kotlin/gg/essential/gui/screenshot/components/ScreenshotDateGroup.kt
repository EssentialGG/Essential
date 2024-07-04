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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.HollowUIContainer
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.screenshot.DateRange
import java.util.*

class ScreenshotDateGroup(
    val range: DateRange,
    val startTime: Long,
) : UIContainer() {

    private var numChildren = BasicState(0)

    private val visible = numChildren.map { it > 0 }

    private val content by UIContainer().constrain {
        width = 100.percent
        height = ChildBasedSizeConstraint()
    }.bindParent(this, visible)

    var textContainer by HollowUIContainer().constrain {
        width = 100.percent
        height = 100.percent
    } effect ScissorEffect()

    private val title by EssentialUIText(range.getName(startTime)).constrain {
        x = CenterConstraint()
    } childOf textContainer

    private val dateDividerContainer by UIContainer().constrain {
        width = 100.percent
        height = verticalScreenshotPadding.pixels
    } childOf content

    private val leftBlock by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        height = 1.pixel
        width = (100.percent - ((CopyConstraintFloat() boundTo title) + 20.pixels)) / 2
        y = CenterConstraint() boundTo title
    } childOf dateDividerContainer

    private val rightBlock by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        x = 0.pixels(alignOpposite = true)
        height = CopyConstraintFloat() boundTo leftBlock
        width = CopyConstraintFloat() boundTo leftBlock
        y = CopyConstraintFloat() boundTo leftBlock
    } childOf dateDividerContainer


    private val images by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedRangeConstraint() + hoverOutlineWidth.pixels
    } childOf content

    init {
        constrain {
            y = SiblingConstraint()
            width = 100.percent
            height = ChildBasedSizeConstraint()
        }

    }

    fun isVisible() = visible.get()

    fun cleanup() {
        // Hide instantly
        numChildren.set(0)

        if (textContainer.hasParent) {
            textContainer.hide(instantly = true)
        }
    }

    fun addScreenshot(screenshot: ScreenshotPreview) {
        screenshot childOf images
        numChildren.set { it + 1 }
    }

    fun getImages(): List<UIComponent> {
        return images.children
    }

    fun isEmpty(): Boolean {
        return numChildren.get() == 0
    }

    fun setupParent(scroller: UIComponent, navigation: UIComponent): ScreenshotDateGroup {
        title.constrain {
            // Position the title in the center of navigation
            y = (CenterConstraint() boundTo navigation)
                // but force it to stay within the content's bounds, so the titles of different groups never overlap
                //an offset of 11 pixels is used in order to position the upper limit at the center of the vertical padding
                .coerceIn(CenterConstraint() boundTo dateDividerContainer, 0.pixels(alignOpposite = true) boundTo content)
        }

        textContainer.bindParent(scroller.parent, visible)
        return this childOf scroller
    }
}