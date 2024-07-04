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

import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.Spacer
import gg.essential.gui.common.WeakState
import gg.essential.gui.sps.InviteFriendsModal
import gg.essential.gui.sps.options.SettingInformation
import gg.essential.gui.sps.options.SpsOption
import gg.essential.util.UUIDUtil
import gg.essential.gui.util.hoveredState
import gg.essential.network.connectionmanager.sps.SPSSessionSource
import gg.essential.util.GuiUtil
import gg.essential.vigilance.utils.onLeftClick

class PlayersAndPermissionsCategory(
    private val cheatsEnabled: State<Boolean>,
) : WorldSettingsCategory(
    "Players & Permissions",
    "No players found",
) {

    // Stored to keep a strong reference so the garbage collector doesn't delete our state listeners
    private val stateListeners = mutableListOf<WeakState<Boolean>>()

    init {
        populate()
    }

    override fun sort() {
        scroller.sortChildren(
            compareBy(
                // Sort first by online status
                {
                    val component = it as? SpsOption ?: return@compareBy Int.MAX_VALUE

                    val uuid = (component.information as SettingInformation.Player).uuid
                    when {
                        // Host
                        uuid == UUIDUtil.getClientUUID() -> 0
                        // Online
                        spsManager.getOnlineState(uuid).get() -> 1
                        // Invited
                        else -> 2
                    }
                },
                // Then sort by name
                {
                    val component = it as? SpsOption ?: return@compareBy Int.MAX_VALUE

                    val uuid = (component.information as SettingInformation.Player).uuid

                    UUIDUtil.getName(uuid).getNow("")
                }
            )
        )
    }

    private fun populate() {
        stateListeners.clear()
        scroller.clearChildren()

        SpsOption.createPlayerOption(SettingInformation.Player(UUIDUtil.getClientUUID()), cheatsEnabled) childOf scroller
        spsManager.invitedUsers.forEach {
            SpsOption.createPlayerOption(SettingInformation.Player(it), cheatsEnabled) childOf scroller
            stateListeners.add(spsManager.getOnlineState(it))
        }

        stateListeners.forEach { weakState ->
            weakState.onSetValue { sort() }
        }

        sort()

        IconButton(EssentialPalette.INVITE_10X6, "Invite").apply {
            val colorState = hoveredState().map {
                if (it) {
                    EssentialPalette.MESSAGE_SENT_HOVER
                } else {
                    EssentialPalette.MESSAGE_SENT
                }
            }
            rebindIconColor(colorState)
            rebindTextColor(colorState)
        }.constrain {
            x = CenterConstraint()
            y = SiblingConstraint(18f)
        }.onLeftClick {
            // The source assumes that this GUI will only be able to be shown
            // from the main menu.
            // Either way, the source shouldn't be used in this case, it's just a side effect
            // of the unfortunate architecture of `InviteFriendsModal`.
            GuiUtil.pushModal { manager ->
                InviteFriendsModal.showInviteModal(manager, source = SPSSessionSource.PAUSE_MENU) { populate() }
            }
        } childOf scroller
        Spacer(height = 10f) childOf scroller
    }

}