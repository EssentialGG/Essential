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
package gg.essential.gui.friends.modal

import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.modals.select.SelectModal
import gg.essential.gui.modals.select.offlinePlayers
import gg.essential.gui.modals.select.onlinePlayers
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.overlay.ModalManager
import gg.essential.util.thenAcceptOnMainThread
import java.util.UUID

class MakeGroupModal(private val socialMenu: SocialMenu) {
    fun create(modalManager: ModalManager): Modal {
        return createSelectFriendsModal(modalManager) { modal, friends ->
            modal.replaceWith(
                createGroupNameModal(
                    modal.modalManager,
                    onCancellation = { replaceWith(create(modalManager)) }
                ) { groupName ->
                    createGroup(friends, groupName)
                }
            )
        }
    }

    private fun createGroup(friends: Set<UUID>, name: String) {
        socialMenu.socialStateManager.messengerStates.createGroup(friends, name).thenAcceptOnMainThread {
            // Intentionally delayed one frame so that the channel preview callback can fire first
            Window.enqueueRenderOperation {
                socialMenu.openMessageScreen(it)
            }
        }
    }

    private fun createGroupNameModal(modalManager: ModalManager, onCancellation: Modal.() -> Unit = {}, onCompletion: (String) -> Unit): Modal {
        return CancelableInputModal(modalManager, "", "", maxLength = 24)
            .configure {
                titleText = "Make Group"
                contentText = "Enter a name for your group."
                primaryButtonText = "Make Group"
                titleTextColor = EssentialPalette.TEXT_HIGHLIGHT

                cancelButtonText = "Back"

                onCancel { onCancellation(this) }
            }
            .mapInputToEnabled { it.isNotBlank() }
            .onPrimaryActionWithValue(onCompletion)
    }

    private fun createSelectFriendsModal(
        modalManager: ModalManager,
        onCancellation: (buttonClicked: Boolean) -> Unit = {},
        onCompletion: (modal: Modal, users: Set<UUID>) -> Unit
    ): SelectModal<UUID> {
        return selectModal(modalManager, "Select Friends") {
            requiresSelection = true
            requiresButtonPress = false

            onlinePlayers()
            offlinePlayers()

            modalSettings {
                primaryButtonText = "Continue"
                cancelButtonText = "Cancel"

                onPrimaryAction { users -> onCompletion(this, users) }
                onCancel(onCancellation)
            }
        }
    }
}
