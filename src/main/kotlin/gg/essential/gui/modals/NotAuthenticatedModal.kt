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
import gg.essential.elementa.state.BasicState
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.not
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.await
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.gui.util.pollingState
import kotlinx.coroutines.launch

class NotAuthenticatedModal(
    modalManager: ModalManager,
    successCallback: Modal.() -> Unit = {}
) : ConfirmDenyModal(modalManager, false) {

    private val connectionManager = Essential.getInstance().connectionManager
    private val connecting = connectionManager.connectionStatus.map { it == null }.toV1(this)
    private val triedConnect = BasicState(false)
    private val authStatus = pollingState { connectionManager.isAuthenticated }
    private val buttonText = connecting.zip(triedConnect).map { (connect, tried) ->
        if (connect) "Connecting..." else if (tried) "Retry" else "Connect"
    }

    init {
        configure {
            contentText = "You are not connected to the Essential Network. This is required to continue."
        }

        bindPrimaryButtonText(buttonText)
        primaryActionButton.rebindEnabled(!connecting)

        // Immediately move on if a connection is established and authenticated
        authStatus.onSetValueAndNow {
            if (it) {
                successCallback.invoke(this)
                replaceWith(null)
            }
        }

        primaryButtonAction = {
            coroutineScope.launch {
                connectionManager.forceImmediateReconnect()
                when (connectionManager.connectionStatus.await { it != null }) {
                    ConnectionManager.Status.SUCCESS -> {}
                    ConnectionManager.Status.MOJANG_UNAUTHORIZED -> {
                        replaceWith(AccountNotValidModal(modalManager, successCallback))
                    }
                    else -> {
                        Notifications.error("Connection Error", "")
                        triedConnect.set(true)
                    }
                }
            }
        }
    }
}
