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
package gg.essential.gui.modals

import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.compactFullEssentialToggle
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.USound
import gg.essential.util.AutoUpdate
import gg.essential.util.MinecraftUtils.shutdown
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class UpdateAvailableModal(modalManager: ModalManager) : ConfirmDenyModal(modalManager, false) {

    init {
        AutoUpdate.dismissUpdateToast?.invoke()

        titleText = AutoUpdate.getNotificationTitle()
        contentTextColor = EssentialPalette.TEXT
        primaryButtonText = "Update"
        primaryButtonStyle = MenuButton.GREEN
        primaryButtonHoverStyle = MenuButton.LIGHT_GREEN

        if (AutoUpdate.requiresUpdate()) {
            titleTextColor = EssentialPalette.MODAL_WARNING
        }

        val autoUpdate = mutableStateOf(AutoUpdate.autoUpdate.get())

        customContent.layoutAsBox(BasicYModifier { SiblingConstraint(17f) }) {
            row {
                spacer(width = 3f)
                row(Arrangement.spacedBy(6f, FloatPosition.START)) {
                    text("Auto-updates", Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(Color.BLACK))
                    box(Modifier.childBasedWidth(3f).childBasedHeight(3f).hoverScope()) {
                        compactFullEssentialToggle(autoUpdate.toV1(this@UpdateAvailableModal))
                        spacer(1f, 1f)
                    }
                }
            }.onLeftClick {
                USound.playButtonPress()
                autoUpdate.set { !it }
            }
        }

        spacer.setHeight(17.pixels)

        AutoUpdate.changelog.whenCompleteAsync({ changelog, _ ->
            changelog?.let { contentText = it }
        }, Window::enqueueRenderOperation)

        onPrimaryAction {
            AutoUpdate.update(autoUpdate.get())

            replaceWith(ConfirmDenyModal(modalManager, true).configure {
                contentText = "Essential will update the next time\nyou launch the game."
                primaryButtonText = "Okay"
                cancelButtonText = "Quit & Update"
                cancelButton.setTooltip("This will close your game!")
            }.onCancel {
                shutdown()
            }.onPrimaryAction {
                Notifications.push("Update Confirmed", "Essential will update next time you launch the game!")
            })
        }

        onCancel { AutoUpdate.ignoreUpdate() }
    }
}

class UpdateRequiredModal(modalManager: ModalManager) : ConfirmDenyModal(modalManager, false) {

    init {
        contentText = "Sorry, you are on an outdated version of Essential. Restart your game to update."
        primaryButtonText = "Quit & Update"
        primaryButtonStyle = MenuButton.DARK_GRAY
        primaryButtonHoverStyle = MenuButton.GRAY
        primaryActionButton.setTooltip("This will close your game!")

        onPrimaryAction { shutdown() }
    }
}
