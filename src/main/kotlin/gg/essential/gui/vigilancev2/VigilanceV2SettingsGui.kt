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
package gg.essential.gui.vigilancev2

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.vigilancev2.palette.VigilancePalette
import gg.essential.util.AutoUpdate
import gg.essential.util.GuiUtil
import gg.essential.vigilance.data.PropertyData

const val bottomBarHeight = 30f

class VigilanceV2SettingsGui @JvmOverloads constructor(
    properties: ListState<PropertyData>,
    initialCategory: String? = null,
): InternalEssentialGUI(ElementaVersion.V6, "Essential Settings") {
    constructor(properties: List<PropertyData>, initialCategory: String? = null) : this(
        stateOf(properties).toListState(),
        initialCategory
    )

    val categories = stateBy {
        properties()
            .groupBy { it.attributesExt.category }
            .map { (name, data) ->
                Category(
                    name,
                    data.groupBy { it.attributesExt.subcategory }
                        .map { (subName, subData) -> SubCategory(subName, subData) }
                )
            }
    }.toListState()

    val defaultCategoryName = initialCategory?.let { name ->
        categories.getUntracked().firstOrNull { it.name.equals(name, ignoreCase = true) }?.name
    } ?: categories.getUntracked().first().name

    val searchState: MutableState<String>
    val currentCategoryName: MutableState<String> = mutableStateOf(defaultCategoryName)

    init {
        titleBar.layout {
            searchState = vigilanceTitleBar(outlineThickness, { leftTitleBarContent() })
        }

        val scrollbar: UIComponent
        rightDivider.layout {
            box(Modifier.width(outlineThickness).fillHeight(topPadding = 30f)) {
                scrollbar = box(Modifier.fillParent().color(VigilancePalette.SCROLLBAR))
            }
        }

        val sidebarScroller: UIComponent

        content.layout {
            sidebarScroller = vigilanceContent(
                outlineThickness,
                categories,
                currentCategoryName,
                searchState,
                scrollbar,
                sidebarSections = listOf(),
                bottomSidebarContent = { bottomSidebarContent() },
            )
        }

        bottomDivider.layout {
            if_(AutoUpdate.updateAvailable) {
                row(
                    Modifier.fillWidth().height(bottomBarHeight).alignVertical(Alignment.End),
                    Arrangement.spacedBy(0f, FloatPosition.START)
                ) {
                    box(Modifier.width(outlineThickness).fillHeight().color(EssentialPalette.LIGHT_DIVIDER))

                    spacer(width = sidebarScroller)

                    box(Modifier.width(outlineThickness).fillHeight().color(EssentialPalette.LIGHT_DIVIDER))
                }
            }
        }
    }

    private fun LayoutScope.leftTitleBarContent() {
    }

    private fun LayoutScope.bottomSidebarContent() {
    }

    override fun updateGuiScale() {
        newGuiScale = GuiUtil.getGuiScale()
        super.updateGuiScale()
    }
}