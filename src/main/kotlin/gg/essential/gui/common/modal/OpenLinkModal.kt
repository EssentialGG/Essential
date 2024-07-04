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
package gg.essential.gui.common.modal

import gg.essential.Essential
import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.toastButton
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.UDesktop
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.net.URI

class OpenLinkModal(modalManager: ModalManager, uri: URI) : ConfirmDenyModal(modalManager, false) {

    init {
        configure {
            titleText = "You are about to visit: "
            primaryButtonText = "Open Browser"
        }
        spacer.setHeight(17.pixels).setY(SiblingConstraint())
        configureLayout {
            val domain by EssentialUIText(
                uri.host,
                truncateIfTooSmall = true,
                showTooltipForTruncatedText = false, // because we set the full url as tooltip below
            ).constrain {
                x = CenterConstraint()
                width = min(width, 100.percent)
                color = EssentialPalette.TEXT.toConstraint()
            }.onLeftClick {
                browse(uri)
            }.bindHoverEssentialTooltip(BasicState(uri.toString()))
            it.insertChildBefore(domain, spacer)
        }
        onPrimaryAction { browse(uri) }
    }

    companion object {
        @JvmStatic
        fun openUrl(uri: URI) {
            val isTrusted =
                TrustedHostsUtil.getTrustedHosts().any { trustedHost -> trustedHost.domains.any { it == uri.host } }

            if (isTrusted) {
                browse(uri, true)
            } else if (EssentialConfig.linkWarning) {
                GuiUtil.pushModal { OpenLinkModal(it, uri) }
            }
        }

        fun browse(uri: URI, successfulToast: Boolean = false) {
            if (!UDesktop.browse(uri)) {
                Essential.logger.error("Failed to open $uri")
                Notifications.pushPersistentToast("Can't open browser", "Unable to open link in browser.", {}, {}) {
                    type = NotificationType.WARNING
                    withCustomComponent(Slot.ACTION, toastButton("Copy Link") {
                        UDesktop.setClipboardString(uri.toString())
                    })
                }
            } else if (successfulToast) {
                Notifications.push("", "Link opened in browser") {
                    withCustomComponent(Slot.ICON, EssentialPalette.JOIN_ARROW_5X.create())
                }
            }
        }
    }
}
