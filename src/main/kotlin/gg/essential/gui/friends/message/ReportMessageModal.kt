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
package gg.essential.gui.friends.message

import com.sparkuniverse.toolbox.chat.model.Message
import gg.essential.Essential
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.Spacer
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.overlay.ModalManager
import gg.essential.universal.UMinecraft
import gg.essential.universal.USound
import gg.essential.util.centered
import gg.essential.vigilance.utils.onLeftClick

class ReportMessageModal(modalManager: ModalManager, val message: Message, username: String) :
    DangerConfirmationEssentialModal(modalManager, confirmText = "Report", requiresButtonPress = false) {

    private val reasonMap = Essential.getInstance().connectionManager.chatManager
        .getReportReasons(UMinecraft.getSettings().language)

    private val reportReasons = reasonMap.values.toList()
    private var reportReason = BasicState(reportReasons[0])

    init {

        configure {
            titleText = "Report $username"
        }

        configureLayout { content ->
            val reasonsContainer by UIContainer().constrain {
                x = CenterConstraint()
                y = SiblingConstraint()
                width = ChildBasedMaxSizeConstraint()
                height = ChildBasedSizeConstraint()
            } childOf content

            reportReasons.forEach { reason ->
                val container by UIContainer().constrain {
                    y = SiblingConstraint(8f)
                    width = ChildBasedSizeConstraint()
                    height = ChildBasedMaxSizeConstraint()
                }.onLeftClick {
                    USound.playButtonPress()
                    reportReason.set(reason)
                } childOf reasonsContainer

                val checkbox by UIBlock().constrain {
                    width = 9.pixels
                    height = AspectConstraint()
                    color = EssentialPalette.BUTTON.toConstraint()
                } childOf container

                val checkmark by EssentialPalette.CHECKMARK_7X5.withColor(EssentialPalette.TEXT)
                    .create().centered().bindParent(checkbox, reportReason.map {
                        it == reason
                    })

                val text by EssentialUIText(reason).constrain {
                    x = SiblingConstraint(5f)
                    color = EssentialPalette.TEXT.toConstraint()
                } childOf container
            }

            // Bottom padding
            Spacer(height = 26f) childOf content
        }

        onPrimaryAction {
            Essential.getInstance().connectionManager.chatManager.fileReport(
                message.channelId,
                message.id,
                reasonMap.entries.first {
                    it.value == reportReason.get()
                }.key
            )
        }
    }
}