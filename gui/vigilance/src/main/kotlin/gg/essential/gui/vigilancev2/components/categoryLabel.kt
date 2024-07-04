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
package gg.essential.gui.vigilancev2.components

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.vigilancev2.Category
import gg.essential.gui.vigilancev2.palette.VigilancePalette
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

fun vigilanceCategoryTextColor(selected: State<Boolean> = stateOf(false)): Modifier {
    return Modifier.withHoverState { hovered ->
        Modifier
            .animateColor(
                stateBy {
                    when {
                        selected() -> VigilancePalette.CATEGORY_TEXT_SELECTED
                        hovered() -> VigilancePalette.CATEGORY_TEXT_HOVERED
                        else -> VigilancePalette.TEXT_LIGHT
                    }
                },
                0.5f
            )
            .whenTrue(
                selected,
                Modifier.shadow(VigilancePalette.CATEGORY_SELECTED_SHADOW),
                Modifier.shadow(VigilancePalette.TEXT_SHADOW)
            )
    }
}

fun LayoutScope.categoryLabel(category: Category, currentCategoryName: MutableState<String>, modifier: Modifier = Modifier) {
    row(Modifier.hoverScope().then(modifier)) {
        val selected = currentCategoryName.map { it == category.name }
        text(category.name, vigilanceCategoryTextColor(selected))
    }.onLeftClick {
        it.stopPropagation()

        USound.playButtonPress()
        currentCategoryName.set(category.name)
    }
}
