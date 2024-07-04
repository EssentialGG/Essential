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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.ChatColor
import gg.essential.util.UUIDUtil
import gg.essential.gui.util.stateBy
import java.util.*

class FirewallBlockingModal(
    modalManager: ModalManager,
    remoteHostName: UUID?,
    cancelAction: Modal.() -> Unit = {},
    tryAgainAction: Modal.() -> Unit,
) : ConfirmDenyModal(modalManager, true) {

    private val message = stateBy {
        if (remoteHostName == null) {
            "The macOS firewall is preventing\nplayers from connecting to your\ncomputer. "
        } else {
            val name = UUIDUtil.getNameAsState(remoteHostName)()
            "The macOS firewall is preventing\nyou from connecting to $name's\ncomputer. "
        } + "To disable this, go to ${ChatColor.WHITE}System Settings > Network >\n" +
            "${ChatColor.WHITE}Firewall > Options ${ChatColor.RESET}and disable\n" +
            "${ChatColor.WHITE}\"Block all incoming connections\"${ChatColor.RESET}."
    }

    init {
        titleText = "Warning!"
        titleTextColor = EssentialPalette.MODAL_WARNING
        contentText = message.get()
        contentTextColor = EssentialPalette.TEXT
        primaryButtonText = "Try Again"
        onPrimaryAction { tryAgainAction.invoke(this) }
        onCancel { cancelAction.invoke(this) }

        textContent.bindText(message)
    }
}
