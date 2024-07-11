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
package gg.essential.gui.wardrobe.categories

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.findChildrenByTag
import gg.essential.gui.util.isInComponentTree
import gg.essential.gui.wardrobe.Item.Companion.toModItem
import gg.essential.gui.wardrobe.Wardrobe.Companion.cosmeticSpacing
import gg.essential.gui.wardrobe.Wardrobe.Companion.cosmeticWidth
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.components.CosmeticItemTag
import gg.essential.gui.wardrobe.components.cosmeticItem
import gg.essential.gui.wardrobe.something.CosmeticGroup
import gg.essential.mod.cosmetics.featured.FeaturedItem
import gg.essential.util.scrollToTopOf

fun LayoutScope.featuredCategory(wardrobeState: WardrobeState, scroller: ScrollComponent, modifier: Modifier = Modifier) {
    val layoutState = wardrobeState.featuredPageLayout

    var content: UIComponent? = null

    bind(layoutState) { (layoutsEmpty, layoutEntry) ->
        if (layoutsEmpty) {
            spacer(height = 20f)
            wrappedText("Error loading featured page!")
            return@bind
        }
        if (layoutEntry == null) {
            spacer(height = 20f)
            wrappedText("Not enough space to show page!\n\nTry making your window wider!")
            return@bind
        }
        val (layoutWidth, layout) = layoutEntry
        val totalHeight = layout.rows.size * 101f + (layout.rows.size - 1).coerceAtLeast(0) * 7f + 2 * cosmeticSpacing
        // Slots that should be left empty because a bigger item spans over them
        val emptySlots = mutableSetOf<Pair<Int, Int>>()

        content = box(Modifier.height(totalHeight).then(modifier)) {
            for ((rowIndex, row) in layout.rows.withIndex()) {
                val verticalPosition = rowIndex * 101f + rowIndex * 7f + cosmeticSpacing
                var itemIndex = 0
                for (columnIndex in 0 until layoutWidth) {
                    // If we ran out of items in this row, break
                    if (itemIndex >= row.size) break

                    // If the slot should be empty, we skip it
                    if (emptySlots.contains(Pair(rowIndex, columnIndex))) continue

                    val horizontalPosition = columnIndex * cosmeticWidth + columnIndex * cosmeticSpacing
                    val featuredItem = row[itemIndex++]
                    val itemWidth = featuredItem.width
                    val itemHeight = featuredItem.height

                    // Add all additional slots this item occupies to the list of empty slots
                    for (w in 0 until itemWidth) {
                        for (h in 0 until itemHeight) {
                            emptySlots.add(Pair(rowIndex + h, columnIndex + w))
                        }
                    }

                    if (featuredItem is FeaturedItem.Empty)
                        continue

                    box(Modifier.alignVertical(Alignment.Start(verticalPosition)).alignHorizontal(Alignment.Start(horizontalPosition))) {
                        bind(featuredItem.toModItem(wardrobeState)) { item ->
                            if (item == null) {
                                text("Error loading item.")
                            } else {
                                cosmeticItem(
                                    item,
                                    WardrobeCategory.FeaturedRefresh,
                                    wardrobeState,
                                    Modifier.width(itemWidth * cosmeticWidth + (itemWidth - 1) * cosmeticSpacing).height(itemHeight * 101f + (itemHeight - 1) * 7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    effect(this.stateScope) {
        val highlightedItem = wardrobeState.highlightItem()

        fun doScroll() {
            if (content?.isInComponentTree() != true) {
                return
            }

            if (highlightedItem == null) {
                return
            }

            scroller.animationFrame() // Force recalculate of position to avoid scrolling an incorrect amount

            val target = content?.findChildrenByTag(CosmeticItemTag::class.java, true) {
                it.item.itemId == highlightedItem
            }?.firstOrNull() ?: return

            scroller.scrollToTopOf(target, offset = -CosmeticGroup.headerHeight)
        }

        // Double delay is needed because this component isn't added to the component tree until the next frame
        // because WardrobeContainer calls layoutSafe() on the current category. Additionally, this listener
        // on the state is called before the component is added to the component tree, so we need to wait for
        // the next frame to scroll to the correct position.
        Window.enqueueRenderOperation { Window.enqueueRenderOperation(::doScroll) }
    }
}