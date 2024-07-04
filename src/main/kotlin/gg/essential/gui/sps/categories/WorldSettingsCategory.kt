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
package gg.essential.gui.sps.categories

import gg.essential.Essential
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.sps.options.SpsOption
import gg.essential.universal.UMinecraft
import gg.essential.util.scrollGradient

@Suppress("LeakingThis")
abstract class WorldSettingsCategory(
    name: String,
    emptyText: String,
    scrollAcceleration: Float = 1.0f,
) : UIContainer() {

    protected val world = UMinecraft.getWorld()!! // World is always present if this UI is opened
    protected val spsManager = Essential.getInstance().connectionManager.spsManager

    private val categoryName by EssentialUIText(name).constrain {
        x = CenterConstraint()
        y = 10.pixels
    } childOf this

    val scroller by ScrollComponent(emptyText, scrollAcceleration = scrollAcceleration).constrain {
        x = CenterConstraint()
        y = SiblingConstraint(9f)
        width = 100.percent - 20.pixels
        height = FillConstraint(useSiblings = false)
    } childOf this scrollGradient 20.pixels

    /**
     * Sorts the options in the scroller. Called after [filter]
     */
    abstract fun sort()

    fun filter(search: String) {
        scroller.filterChildren { component ->
            when (component) {
                is SpsOption -> {
                   component.information.matchesFilter(search)
                }
                is GameRulesCategory.CategoryComponent -> {
                    component.filterChildren(search)
                }
                else -> {
                    true
                }
            }
        }
        sort()
    }

    init {
        constrain {
            width = (100.percent - 6.pixels) / 3
            height = 100.percent
            x = SiblingConstraint()
        }
    }
}