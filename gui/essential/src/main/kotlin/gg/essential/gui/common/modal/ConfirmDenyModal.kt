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

import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.overlay.ModalManager

/**
 * Template for a modal giving the user a choice to confirm or deny an action
 */
@Deprecated(
    "Using ConfirmDenyModal is discouraged, use EssentialModal2 instead.",
    replaceWith = ReplaceWith("EssentialModal2"),
)
open class ConfirmDenyModal(
    modalManager: ModalManager,
    requiresButtonPress: Boolean,
) : EssentialModal(
    modalManager,
    requiresButtonPress,
) {

    private val cancelButtonTextState = BasicState("Cancel").map { it }

    private val cancelActions = mutableListOf<(Boolean) -> Unit>()

    var cancelButtonText: String
        get() = cancelButtonTextState.get()
        set(value) = cancelButtonTextState.set(value)

    val cancelButton by MenuButton(cancelButtonTextState, hoverStyle = BasicState(MenuButton.GRAY)) {
        fireCancel(true)
        replaceWith(null)
    }.constrain {
        width = CopyConstraintFloat() boundTo primaryActionButton
        height = 20.pixels
    }

    // Top padding
    val spacer by Spacer(height = 14f) childOf customContent

    init {
        primaryButtonStyle = MenuButton.BLUE
        primaryButtonHoverStyle = MenuButton.LIGHT_BLUE
        primaryButtonDisabledStyle = MenuButton.BLUE_DISABLED
        buttonContainer.insertChildBefore(cancelButton, primaryActionButton)

        primaryActionButton.constrain {
            x = SiblingConstraint(8f)
        }
        configure {
            titleTextColor = EssentialPalette.TEXT_HIGHLIGHT
        }

        super.onPrimaryOrDismissAction {
            if (!it) {
                fireCancel(false)
            }
        }
    }

    /**
     * Called on cancel with the boolean argument set to true if the user presses the dismiss button and false if they click out of bounds
     */
    fun onCancel(callback: (Boolean) -> Unit) = apply {
        cancelActions.add(callback)
        return this
    }

    private fun fireCancel(buttonClicked: Boolean) {
        for (cancelAction in cancelActions) {
            cancelAction(buttonClicked)
        }
    }

    /**
     * Overridden with hard fail to avoid misuse
     */
    @Deprecated(
        "onPrimaryOrDismissAction is unavailable for ConfirmDenyModal. Use Either onCancel() or onPrimaryAction()",
        level = DeprecationLevel.HIDDEN
    )
    override fun onPrimaryOrDismissAction(callback: (Boolean) -> Unit): EssentialModal {
        throw IllegalStateException("onPrimaryOrDismissAction is unavailable for ConfirmDenyModal. Use Either onCancel() or onPrimaryAction()")
    }

    fun onPrimaryAction(callback: () -> Unit) = apply {
        super.onPrimaryOrDismissAction {
            if (it) {
                callback()
            }
        }
        return this
    }

    fun hideCancelButton() = cancelButton.hide(true)
}
