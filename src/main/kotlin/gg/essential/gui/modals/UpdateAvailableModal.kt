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
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.compactFullEssentialToggle
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.state
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
        contentTextSpacingState.rebind(BasicState(17f))

        if (AutoUpdate.requiresUpdate()) {
            titleTextColor = EssentialPalette.MODAL_WARNING
        }

        val autoUpdate = mutableStateOf(AutoUpdate.autoUpdate.get())

        customContent.layoutAsBox(BasicYModifier { SiblingConstraint(15f) }) {
            column {
                row(
                    Modifier.childBasedWidth(3f).onLeftClick {
                        USound.playButtonPress()
                        autoUpdate.set { !it }
                    },
                    Arrangement.spacedBy(9f),
                ) {
                    text("Auto-updates", Modifier.color(EssentialPalette.TEXT_DISABLED).shadow(Color.BLACK))
                    box(Modifier.childBasedHeight(3f).hoverScope()) {
                        compactFullEssentialToggle(
                            autoUpdate.toV1(this@UpdateAvailableModal),
                            offColor = EssentialPalette.TEXT_DISABLED.state()
                        )
                        spacer(1f, 1f)
                    }
                }

                spacer(height = 14f)
            }
        }

        spacer.setHeight(0.pixels)

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
