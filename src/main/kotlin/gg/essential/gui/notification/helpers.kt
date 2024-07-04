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
package gg.essential.gui.notification

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.gui.EssentialPalette

fun sendTosNotification(viewButtonAction: () -> Unit) {
    Notifications.pushPersistentToast(
        "Terms of Service",
        "This feature requires you to accept the Essential ToS.",
        action = {},
        close = {},
    ) {
        uniqueId = object {}.javaClass
        withCustomComponent(Slot.ICON, EssentialPalette.ROUND_WARNING_7X.create())
        withCustomComponent(Slot.ACTION, toastButton("View", action = viewButtonAction))
    }
}

fun sendCheckoutFailedNotification() {
    Notifications.pushPersistentToast(
        "Error",
        "An issue occurred while trying to send you to checkout. Please try again later.",
        {},
        {},
    ) {
        type = NotificationType.ERROR
    }
}