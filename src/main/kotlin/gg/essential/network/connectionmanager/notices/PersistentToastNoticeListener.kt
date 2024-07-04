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
package gg.essential.network.connectionmanager.notices

import gg.essential.Essential
import gg.essential.event.gui.GuiOpenEvent
import gg.essential.gui.notification.Notifications
import gg.essential.gui.wardrobe.Wardrobe
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.network.connectionmanager.notices.NoticesManager.NoticeListener
import gg.essential.network.connectionmanager.telemetry.TelemetryManager
import gg.essential.notices.NoticeType
import gg.essential.notices.model.Notice
import gg.essential.universal.UMinecraft
import gg.essential.util.GuiUtil
import gg.essential.util.Multithreading
import gg.essential.util.isMainMenu
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.Minecraft
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

class PersistentToastNoticeListener(
    private val noticesManager: NoticesManager,
) : NoticeListener {

    private val pushedToasts = mutableMapOf<Notice, () -> Unit>()
    private val notices = mutableSetOf<Notice>()

    private val actions = mapOf(
        "OPEN_EMOTES" to { GuiUtil.openScreen { Wardrobe(initialCategory = WardrobeCategory.Emotes) } },
    )

    init {
        Essential.EVENT_BUS.register(this)
    }

    override fun noticeAdded(notice: Notice) {
        if (notice.type != NoticeType.DISMISSIBLE_TOAST) {
            return
        }
        notices.add(notice)
        if (Minecraft.getMinecraft().currentScreen.isMainMenu) {
            pushNoticeToast(notice)
        }
    }

    private fun pushNoticeToast(notice: Notice) {
        val title = notice.metadata["title"] as? String ?: return
        val message = notice.metadata["message"] as? String ?: return
        val action = notice.metadata["action"] as? String

        val telemetryManager = Essential.getInstance().connectionManager.telemetryManager

        val pushNotice = {
            // Send a dismissible toast with the title and message
            Notifications.pushPersistentToast(title, message, {
                // When the toast is clicked
                // FIXME: This is hard coded for now until we have a better way to handle this
                actions[action]?.let { it() }
                dismissToast(notice)
                telemetryManager.clientActionPerformed(TelemetryManager.Actions.PERSISTENT_TOAST_CLICKED, notice.id)
            }, {
                // When the [x] is pressed. We need to check pushedToasts to make sure
                // this is not called by the toast being dismissed due to the screen changing
                if (notice in pushedToasts.keys) {
                    dismissToast(notice)
                    telemetryManager.clientActionPerformed(TelemetryManager.Actions.PERSISTENT_TOAST_CLEARED, notice.id)
                }

            }) {
                pushedToasts[notice] = this.dismissNotificationInstantly
            }
        }

        val activeAfter = notice.activeAfter ?: return
        if (activeAfter.after(Date.from(Instant.now()))) {
            pushNotice()
        } else {
            Multithreading.scheduleOnMainThread(pushNotice, Instant.now().until(activeAfter.toInstant(), ChronoUnit.MILLIS), TimeUnit.MILLISECONDS)
        }

        notice.expiresAt?.let { expiresAt ->
            Multithreading.scheduleOnMainThread({
                noticeRemoved(notice)
            }, Instant.now().until(expiresAt.toInstant(), ChronoUnit.MILLIS), TimeUnit.MILLISECONDS)
        }
    }

    @Subscribe
    fun guiOpenEvent(event: GuiOpenEvent) {
        if ((event.gui.isMainMenu || event.gui == null) && UMinecraft.getWorld() == null) {
            notices.forEach { notice ->
                // Check the notice is not already showing
                if (notice !in pushedToasts.keys) {
                    pushNoticeToast(notice)
                }
            }
        } else {
            hideAllToasts()
        }
    }

    private fun hideAllToasts() {
        pushedToasts.entries.toMutableSet().forEach { (notice, dismissAction) ->
            pushedToasts.remove(notice)
            dismissAction()
        }
    }

    private fun dismissToast(notice: Notice) {
        noticesManager.dismissNotice(notice.id)
        notices.remove(notice)
    }

    override fun noticeRemoved(notice: Notice) {
        notices.remove(notice)
        pushedToasts.remove(notice)?.let { dismiss ->
            dismiss()
        }
    }

    override fun onConnect() {
        hideAllToasts()
        notices.clear()
    }
}