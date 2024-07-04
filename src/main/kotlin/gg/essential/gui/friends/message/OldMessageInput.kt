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

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.input.UIMultilineTextInput
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.friends.message.v2.ReplyableMessageScreen
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.warning
import gg.essential.universal.ChatColor
import gg.essential.universal.UKeyboard
import gg.essential.vigilance.utils.onLeftClick

open class OldMessageInput(
    private val groupName: State<String>,
    editingMessage: State<ClientMessage?>,
    private val replyableMessageScreen: ReplyableMessageScreen,
) : UIContainer() {
    private lateinit var onSendAction: (String) -> Unit

    val messageEditingState = editingMessage.map { it != null }

    protected val topDivider by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        width = 100.percent
        height = 3.pixels
    } childOf this

    private val inputBlock by UIBlock(EssentialPalette.INPUT_BACKGROUND).constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = 30.pixels
    } childOf this

    internal val input by UIMultilineTextInput(shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT).constrain {
        x = 10.pixels
        y = CenterPixelConstraint()
        width = 100.percent - 63.pixels
        height = 10.pixels
    }.apply {
        groupName.onSetValueAndNow {
            placeholder = "Message $it"
        }
        setMaxLines(2)
        setColor(basicColorConstraint {
            if (getText().isEmpty()) {
                EssentialPalette.TEXT
            } else {
                EssentialPalette.TEXT_HIGHLIGHT
            }
        })
    } childOf inputBlock

    private val sendButtonEnabled = BasicState(false)

    private val imageContainer by IconButton(
        EssentialPalette.ARROW_RIGHT_4X7,
        buttonText = "",
        tooltipText = "Send",
    ).constrain {
        x = 10.pixels(alignOpposite = true)
        y = CenterConstraint()
        width = 17.pixels
        height = AspectConstraint()
    }.apply {
        onLeftClick {
            handleSendMessage()
        }
        rebindEnabled(sendButtonEnabled)
    } childOf inputBlock


    init {

        constrain {
            y = SiblingConstraint()
            width = 100.percent
            height = ChildBasedSizeConstraint()
        }

        onLeftClick {
            grabFocus()
        }

        input.onKeyType { _, keyCode ->
            if (keyCode == UKeyboard.KEY_ENTER) {
                if (!UKeyboard.isShiftKeyDown()) {
                    handleSendMessage()
                }
            }
        }

        input.onUpdate {
            sendButtonEnabled.set(it.isNotEmpty())
        }
    }

    fun grabFocus() {
        input.grabWindowFocus()
    }

    private fun handleSendMessage() {
        var text = input.getText()
        val charLimit = 500
        if (text.length > charLimit) {
            Notifications.warning("Too many characters", "You have exceeded the\n$charLimit character limit.")
            return
        }
        //Keep calling the replacement until there are no more changes
        //&sect&sect;; will pass the filter because on the first pass
        // &sect; is replaced but still leaves &sect;
        var previous = text
        var modified = false
        do {
            for (colorSymbol in colorSymbols) {
                text = text.replace(colorSymbol, "")
            }
            if (text != previous) {
                previous = text
                modified = true
            } else {
                modified = false
            }
        } while (modified)

        if (messageEditingState.get()) {
            replyableMessageScreen.editMessage(text)
        } else {
            onSendAction(text)
            input.setText("")
        }
    }

    override fun afterInitialization() {
        input.grabWindowFocus()
    }

    companion object {
        val colorSymbols = listOf("&sect;", "&#xA7", "&#167", "${ChatColor.COLOR_CHAR}")
    }
}
