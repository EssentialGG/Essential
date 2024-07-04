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
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IntEssentialSlider
import kotlin.math.abs

class ScreenshotItemsSlider(
    private val itemsPerRow: State<Int>,
    private val listViewComponent: ListViewComponent
) : UIContainer() {

    private var scrollTo: UIComponent? = null
    private val sliderWidth = 36
    private val bigItemsIcon by EssentialPalette.IMAGE_SIZE_BIG_10X.create().constrain {
        // Should be CenterConstraint() but the image is larger than its content
        // and we want the content centered
        y = 1.pixel
    } childOf this

    private val slider by IntEssentialSlider(minItemsPerRow, maxItemsPerRow, itemsPerRow.get()).constrain {
        x = SiblingConstraint(3f)
        y = CenterConstraint()
        width = sliderWidth.pixels
        height = 9.pixels
    } childOf this



    private val smallItemsIcon by EssentialPalette.IMAGE_SIZE_SMALL_9X.create().constrain {
        x = SiblingConstraint(3f)
        y = CenterConstraint()
    } childOf this

    init {
        constrain {
            width = ChildBasedSizeConstraint(3f)
            height = 11.pixels
        }
        slider.onUpdateInt {
            handleUpdateItems(it)
        }
    }

    override fun afterInitialization() {
        super.afterInitialization()

        var previousVerticalOffset = 0f
        // Need to wait for all the other components to be initialized
        val screenshotScrollComponent = listViewComponent.screenshotBrowser.listViewComponent.screenshotScrollComponent
        screenshotScrollComponent.addScrollAdjustEvent(true) { _: Float, _: Float ->
            if(screenshotScrollComponent.verticalOffset != previousVerticalOffset) {
                previousVerticalOffset = screenshotScrollComponent.verticalOffset
                scrollTo = null
            }
        }
    }


    /**
     * Somewhat of a hack such that we can keep the items in the center of the screen
     * in the same place after updating the number of items per row
     */
    private fun handleUpdateItems(itemsPerRow: Int) {
        fun UIComponent.center(): Float {
            return (getTop() + getBottom()) / 2
        }

        val previousScroll = scrollTo

        val scroller = listViewComponent.screenshotScrollComponent

        val scrollerCenter = scroller.center()
        val closest = previousScroll ?: scroller.allChildren
            .flatMap { (it as ScreenshotDateGroup).getImages() }
            .minByOrNull { abs(scrollerCenter - it.center()) }


        if (closest != null) {
            val top = closest.getTop()
            val offset = scroller.verticalOffset
            scrollTo = closest

            this.itemsPerRow.set(itemsPerRow)
            listViewComponent.screenshotBrowser.window.animationFrame()
            scroller.scrollTo(verticalOffset = offset - (closest.getTop() - top), smoothScroll = false)
            scrollTo = closest
        } else {
            this.itemsPerRow.set(itemsPerRow)
        }
    }
}