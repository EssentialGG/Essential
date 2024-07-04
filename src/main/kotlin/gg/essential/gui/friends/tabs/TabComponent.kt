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
package gg.essential.gui.friends.tabs

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.gui.common.HollowUIContainer
import gg.essential.gui.common.bindParent
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.friends.Tab
import gg.essential.gui.friends.previews.SearchableItem

abstract class TabComponent(
    protected val tab: Tab,
    protected val gui: SocialMenu,
    selectedTab: State<Tab>
) : HollowUIContainer() {

    val stateManager = gui.socialStateManager
    val messengerStates = stateManager.messengerStates
    val relationshipStates = stateManager.relationshipStates

    val active = selectedTab.map { it == tab }

    /**
     * All scrollable lists of users (components implementing [SearchableItem]) of this tab.
     *
     * These will automatically be filtered when the user uses the search bar.
     */
    abstract val userLists: List<ScrollComponent>

    init {
        bindParent(gui.content, active)
        constrain {
            width = 100.percent
            height = 100.percent
        }
    }

    abstract fun populate()

    fun search(username: String?) {
        val lowerCase = username?.lowercase()
        for (list in userLists) {
            list.filterChildren {
                lowerCase == null || it !is SearchableItem || lowerCase in it.getSearchTag().lowercase()
            }
        }
    }
}
