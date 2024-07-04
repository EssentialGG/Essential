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
package gg.essential.gui.wardrobe.modals

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.coins.CoinsManager
import gg.essential.universal.ChatColor

class PurchaseConfirmModal(
    modalManager: ModalManager,
    modalText: String,
    cost: Int?,
    primaryAction: () -> Unit,
) : ConfirmDenyModal(modalManager, false) {

    private val modalContent by UIContainer().constrain {
        x = CenterConstraint()
        y = 1.pixel
        width = 100.percent
        height = ChildBasedSizeConstraint() - 1.pixel
    }.bindParent(content, stateOf(true), false, 2)

    private val textModifier = if (ChatColor.COLOR_CODE_PATTERN.containsMatchIn(modalText)) Modifier else Modifier.color(EssentialPalette.TEXT)

    init {
        modalContent.layout {
            column(Modifier.fillWidth(), Arrangement.spacedBy(10f)) {
                wrappedText(modalText, textModifier.shadow(EssentialPalette.BLACK), true)
                row(Arrangement.spacedBy(2f)) {
                    text(CoinsManager.COIN_FORMAT.format(cost), Modifier.shadow(EssentialPalette.BLACK))
                    icon(EssentialPalette.COIN_7X, Modifier.shadow(EssentialPalette.BLACK))
                }
            }
        }

        primaryButtonText = "Purchase"
        onPrimaryAction(primaryAction)
    }
}
