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

import gg.essential.Essential
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.overlay.ModalManager
import gg.essential.handlers.account.WebAccountManager
import gg.essential.gui.util.pollingState

class AccountNotValidModal(
    modalManager: ModalManager,
    successCallback: Modal.() -> Unit = {}
) : ConfirmDenyModal(modalManager, false) {

    private val authStatus = pollingState { Essential.getInstance().connectionManager.isAuthenticated }

    init {
        configure {
            contentText = "Something went wrong or your account is not authenticated with Essential. Log into your account on our website to securely add it in-game."
            primaryButtonText = "Open Browser"
        }

        // Immediately move on if a connection is established and authenticated
        authStatus.onSetValueAndNow {
            if (it) {
                successCallback.invoke(this)
                replaceWith(null)
            }
        }

        onPrimaryAction {
            WebAccountManager.openInBrowser()
        }
    }
}
