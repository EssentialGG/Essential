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
package gg.essential.gui.friends.message.v2

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.pixels
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.or
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.friends.message.OldMessageInput
import gg.essential.gui.util.hoveredState
import gg.essential.universal.UKeyboard
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick

class ReplyMessageInput(
    groupName: State<String>,
    private val replyTo: State<ClientMessage?>,
    private val editingMessage: State<ClientMessage?>,
    replyableMessageScreen: ReplyableMessageScreen,
) : OldMessageInput(groupName, editingMessage, replyableMessageScreen) {

    private val isReplying = replyTo.map { it != null }
    private val showActionBar = isReplying or messageEditingState

    private val actionBarIcon = replyTo.zip(editingMessage).map { (replyMessage, editMessage) ->
        if (replyMessage != null) {
            EssentialPalette.REPLY_7X5
        } else if (editMessage != null) {
            EssentialPalette.PENCIL_7x7
        } else {
            EssentialPalette.NONE
        }
    }
    private val actionBarText = replyTo.zip(editingMessage).map { (replyMessage, editMessage) ->
        if (replyMessage != null) {
            "Replying to "
        } else if (editMessage != null) {
            "Editing Message"
        } else {
            ""
        }
    }

    private var stashedMessage: String? = ""

    private val usernameState = BasicState("").map { it }.apply {
        replyTo.onSetValue {
            if (it != null) {
                rebind(UUIDUtil.getNameAsState(it.sender))
            }
        }
    }

    private val actionBarContainer by UIContainer().constrain {
        y = CenterConstraint()
        height = ChildBasedMaxSizeConstraint()
        width = 100.percent
    }.bindParent(topDivider, showActionBar)

    private val actionIcon by ShadowIcon(actionBarIcon, BasicState(true)).constrain {
        x = 10.pixels
        y = CenterConstraint()
    } childOf actionBarContainer

    private val actionText by EssentialUIText(shadowColor = EssentialPalette.TEXT_SHADOW).bindText(actionBarText).constrain {
        x = SiblingConstraint(4f)
        y = CenterConstraint()
        color = EssentialPalette.TEXT_MID_GRAY.toConstraint()
    } childOf actionBarContainer

    private val usernameText by EssentialUIText(shadowColor = EssentialPalette.TEXT_SHADOW).bindText(usernameState).constrain {
        x = SiblingConstraint() // Padding between components is handled by the space in the previous text
        y = CenterConstraint()
        color = EssentialPalette.TEXT_HIGHLIGHT.toConstraint()
    }.bindParent(actionBarContainer, isReplying, index = 2)

    // Makes the hit box for the icon larger so its easier to click
    private val closeIconContainer by UIContainer().constrain {
        y = CenterConstraint()
        x = 14.pixels(alignOpposite = true)
        width = ChildBasedSizeConstraint() + 4.pixels
        height = ChildBasedSizeConstraint() + 4.pixels
    }.onLeftClick {
        if (isReplying.get()) {
            replyTo.set(null)
        } else if (messageEditingState.get()) {
            editingMessage.set(null)
        }
        USound.playButtonPress()
    }.bindHoverEssentialTooltip(BasicState("Cancel")) childOf actionBarContainer

    private val closeIcon by EssentialPalette.CANCEL_5X.create().centered().constrain {
        color = EssentialPalette.getTextColor(closeIconContainer.hoveredState()).toConstraint()
    } childOf closeIconContainer

    init {
        replyTo.onSetValue {
            if (it != null) {
                grabFocus()
            }
        }
        editingMessage.onSetValue {
            if (it != null) {
                grabFocus()
                if (stashedMessage == null) {
                    stashedMessage = input.getText()
                }
                input.setText(it.contents)
            } else {
                stashedMessage?.let { text -> input.setText(text) }
                stashedMessage = ""
            }
        }

        topDivider.constrain {
            height += showActionBar.map {
                if (it) {
                    ACTION_BAR_HEIGHT
                } else {
                    0f
                }
            }.pixels()
        }.onLeftClick {
            val message = replyTo.get() ?: editingMessage.get()
            message?.let { replyableMessageScreen.scrollToMessage(it) }
        }

        // Cancel reply on escape
        input.onKeyType { _, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                if (isReplying.get()) {
                    replyTo.set(null)
                } else if (messageEditingState.get()) {
                    editingMessage.set(null)
                } else {
                    replyableMessageScreen.preview.gui.restorePreviousScreen()
                }
            }
        }
    }

    companion object {
        const val ACTION_BAR_HEIGHT = 14f
    }
}
