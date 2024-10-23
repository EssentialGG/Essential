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
package gg.essential.util

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.toastButton
import gg.essential.universal.UDesktop
import java.net.URI

fun openInBrowser(uri: URI) {
    if (UDesktop.browse(uri)) {
        Notifications.push("", "Link opened in browser") {
            withCustomComponent(Slot.ICON, EssentialPalette.JOIN_ARROW_5X.create())
        }
    } else {
        Notifications.pushPersistentToast("Can't open browser", "Unable to open link in browser.", {}, {}) {
            type = NotificationType.WARNING
            withCustomComponent(Slot.ACTION, toastButton("Copy Link") {
                UDesktop.setClipboardString(uri.toString())
            })
        }
    }
}
