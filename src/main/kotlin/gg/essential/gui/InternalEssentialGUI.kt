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
package gg.essential.gui

import gg.essential.Essential
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.EssentialGUI
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.elementa.ElementaVersion
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.universal.UMatrixStack

abstract class InternalEssentialGUI(
    version: ElementaVersion,
    guiTitle: String,
    newGuiScale: Int = EssentialAPI.getGuiUtil().getGuiScale(),
    restorePreviousGuiOnClose: Boolean = true,
    discordActivityDescription: String? = null,
): EssentialGUI(version, guiTitle, newGuiScale, restorePreviousGuiOnClose, discordActivityDescription) {
    private val screenOpenMutable = mutableStateOf(false)
    protected val screenOpen: State<Boolean> = screenOpenMutable

    private var openedAt: Long? = null

    override fun initScreen(width: Int, height: Int) {
        super.initScreen(width, height)

        // Note: initScreen also gets called on resize, but since the state will already be `true`, this is fine
        screenOpenMutable.set(true)
    }

    override fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)

        if (openedAt == null) {
            openedAt = System.currentTimeMillis()
        }
    }

    override fun onScreenClose() {
        super.onScreenClose()

        sendSessionTelemetry()
        screenOpenMutable.set(false)
    }

    private fun sendSessionTelemetry() {
        val duration = openedAt?.let { System.currentTimeMillis() - it } ?: return
        openedAt = null

        Essential.getInstance().connectionManager.telemetryManager.enqueue(
            ClientTelemetryPacket(
                "GUI_SESSION_DURATION",
                mapOf("gui" to this.javaClass.name, "durationSeconds" to duration / 1000)
            )
        )
    }
}