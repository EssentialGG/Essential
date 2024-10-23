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
package gg.essential.config

import com.sparkuniverse.toolbox.relationships.enums.FriendRequestPrivacySetting
import gg.essential.Essential
import gg.essential.commands.EssentialCommandRegistry
import gg.essential.config.EssentialConfig.autoUpdate
import gg.essential.config.EssentialConfig.autoUpdateState
import gg.essential.config.EssentialConfig.discordRichPresenceState
import gg.essential.config.EssentialConfig.essentialEnabledState
import gg.essential.config.EssentialConfig.friendRequestPrivacyState
import gg.essential.config.EssentialConfig.ownCosmeticsHiddenStateWithSource
import gg.essential.connectionmanager.common.packet.relationships.privacy.FriendRequestPrivacySettingPacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.data.OnboardingData
import gg.essential.data.OnboardingData.hasAcceptedTos
import gg.essential.elementa.components.Window
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.modal.discord.DiscordActivityStatusModal
import gg.essential.gui.modals.TOSModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.sendTosNotification
import gg.essential.gui.vigilancev2.VigilanceV2SettingsGui
import gg.essential.util.AutoUpdate
import gg.essential.util.GuiUtil

object McEssentialConfig {
    private val referenceHolder = ReferenceHolderImpl()

    @JvmOverloads
    fun gui(initialCategory: String? = null): VigilanceV2SettingsGui = VigilanceV2SettingsGui(EssentialConfig.gui, initialCategory)

    fun hookUp() {
        EssentialConfig.doRevokeTos = ::revokeTos

        friendRequestPrivacyState.onSetValue(referenceHolder) { it ->
            if (hasAcceptedTos()) {
                val connectionManager = Essential.getInstance().connectionManager
                val privacy = FriendRequestPrivacySetting.values()[it]

                connectionManager.send(FriendRequestPrivacySettingPacket(privacy)) {
                    val get = it.orElse(null)
                    if (get == null || !(get is ResponseActionPacket && get.isSuccessful)) {
                        Notifications.error("Error", "An unexpected error occurred. Please try again.")
                    }
                }
            }
        }

        ownCosmeticsHiddenStateWithSource.onSetValue(referenceHolder) { (hidden, setByUser) ->
            if (Essential.getInstance().connectionManager.isAuthenticated) {
                Essential.getInstance().connectionManager.cosmeticsManager.setOwnCosmeticVisibility(false, !hidden)
            } else {
                if (!setByUser) return@onSetValue // infra/mod may set whatever it wants, only the user is getting checked
                if (hasAcceptedTos()) {
                    Notifications.error(
                        "Essential Network Error",
                        "Unable to establish connection with the Essential Network."
                    )
                } else {
                    fun showTOS() = GuiUtil.pushModal { TOSModal(it, unprompted = false, requiresAuth = true, {}) }
                    if (GuiUtil.openedScreen() == null) {
                        // Show a notification when we're not in any menu, so it's less intrusive
                        sendTosNotification { showTOS() }
                    } else {
                        showTOS()
                    }
                }
                EssentialConfig.ownCosmeticsHidden = !hidden
            }
        }

        discordRichPresenceState.onSetValue(referenceHolder) { enabled ->
            if (!enabled) return@onSetValue

            GuiUtil.pushModal { DiscordActivityStatusModal(it) }
        }

        essentialEnabledState.onSetValue(referenceHolder) { enabling ->
            Window.enqueueRenderOperation { toggleEssential(enabling) }
        }

        autoUpdate = AutoUpdate.autoUpdate.get()
        autoUpdateState.onSetValue(referenceHolder) { shouldAutoUpdate ->
            if (shouldAutoUpdate != AutoUpdate.autoUpdate.get()) {
                // User explicitly changed the value
                // Delayed to allow setAutoUpdate to confirm the value of the autoUpdate setting
                Window.enqueueRenderOperation {
                    AutoUpdate.setAutoUpdates(shouldAutoUpdate)
                }
            }
        }
    }

    private fun checkSPS(): Boolean {
        return if (Essential.getInstance().connectionManager.spsManager.localSession != null) {
            Notifications.error("Error", "You cannot disable Essential while hosting a world.")
            false
        } else true
    }

    private fun toggleEssential(enabling: Boolean) {
        // Trying to disable Essential while in an SPS world
        if (!enabling && !checkSPS()) {
            EssentialConfig.essentialEnabled = true
            return
        }

        EssentialConfig.essentialEnabled = enabling

        Essential.getInstance().keybindingRegistry.refreshBinds()
        (Essential.getInstance().commandRegistry() as EssentialCommandRegistry).checkMiniCommands()
        Essential.getInstance().checkListeners()

        if (!enabling) {
            Essential.getInstance().connectionManager.onTosRevokedOrEssentialDisabled()
        }
    }

    private fun revokeTos() {
        if (checkSPS()) {
            OnboardingData.setDeniedTos()
            Essential.getInstance().connectionManager.onTosRevokedOrEssentialDisabled()
        }
    }
}
