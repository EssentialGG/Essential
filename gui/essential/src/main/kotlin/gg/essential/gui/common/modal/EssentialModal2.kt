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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.StyledButton
import gg.essential.gui.common.styledButton
import gg.essential.gui.common.textStyle
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.WrappedTextBuilder
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedSize
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.onLeftClick
import gg.essential.gui.layoutdsl.outline
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.tag
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.layoutdsl.wrappedText
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.Focusable
import gg.essential.gui.util.Tag
import gg.essential.gui.util.findChildrenByTag
import gg.essential.gui.util.focusable
import gg.essential.gui.util.simulateLeftClick
import gg.essential.universal.UKeyboard
import gg.essential.universal.USound
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.launch
import java.awt.Color

/**
 * A base-modal with some design defaults.
 * Intended to be the replacement for [EssentialModal].
 */
abstract class EssentialModal2(
    modalManager: ModalManager,
    private val requiresButtonPress: Boolean = false,
) : Modal(modalManager) {
    /**
     * Listens for keyboard navigation events on the modal's [Window].
     *
     * Only the component which holds the [Window]'s focus receives key events, so if this listener is being used,
     * that means that no component currently holds that focus.
     */
    private val keyEventListener: UIComponent.(Char, Int) -> Unit = keyListener@{ _, keyCode ->
        val window = Window.of(this)

        when (keyCode) {
            /** Passing focus from one element to another is done via [gg.essential.gui.util.setupKeyboardNavigation]. */
            UKeyboard.KEY_TAB -> {
                val focusable = window.findChildrenByTag<Focusable>(recursive = true) { !it.disabled.getUntracked() }

                val nextComponent = when {
                    UKeyboard.isShiftKeyDown() -> focusable.lastOrNull()
                    else -> focusable.firstOrNull()
                }

                nextComponent?.grabWindowFocus()
            }

            UKeyboard.KEY_ENTER -> {
                val primaryAction = window.findChildrenByTag<PrimaryAction>(recursive = true).singleOrNull()

                // The simulated left-click event may cause the component's hierarchy to change.
                // In order to prevent a CME, we need to use `enqueueRenderOperation`.
                Window.enqueueRenderOperation {
                    primaryAction?.simulateLeftClick()
                }
            }
        }
    }

    init {
        @Suppress("LeakingThis")
        platform.dismissModalOnScreenChange(this, ::close)
    }

    override fun onOpen() {
        super.onOpen()

        Window.of(this).onKeyType(keyEventListener)
    }

    override fun onClose() {
        // The location of this does not make a difference with the current implementation, but we should
        // close things in the reverse order in which they were opened.
        // i.e. We added our key listener after `Modal` added theirs, so we remove ours before they remove theirs.
        Window.of(this).keyTypedListeners.remove(keyEventListener)

        super.onClose()
    }

    override fun LayoutScope.layoutModal() {
        box(
            Modifier
                .childBasedSize(16f)
                .color(EssentialPalette.MODAL_BACKGROUND)
                .outline(EssentialPalette.BUTTON_HIGHLIGHT, 1f)
        ) {
            column {
                // The spacing between the top of the modal to the content, and the bottom of the modal to the content
                // is un-even. We need to add an extra 3 pixels at the top.
                spacer(height = 3f)
                layoutContent()
            }
        }
    }

    open fun LayoutScope.layoutContent(modifier: Modifier = Modifier) = layoutContentImpl(modifier)

    /**
     * This is basically the "default implementation" for `layoutContent`.
     * If somebody wants to override `layoutContent` to just change something on the `modifier`, they can
     * override `layoutContent` and call this.
     *
     * We can't make `layoutContent` take a `Modifier`, and make the implementation call on `super`, since you can't
     * call a superclass' implementation of an extension function. ([KT-11488](https://youtrack.jetbrains.com/issue/KT-11488/When-overriding-a-member-extension-function-cannot-call-superclass-implementation))
     */
    fun LayoutScope.layoutContentImpl(modifier: Modifier = Modifier) {
        column(Modifier.width(190f).then(modifier), Arrangement.spacedBy(17f)) {
            layoutTitle()
            layoutBody()
            layoutButtons()
        }
    }

    /** Mainly intended for the [title] of your modal. */
    open fun LayoutScope.layoutTitle() {}

    /** For the actual content of your modal, e.g. the [description] text. */
    open fun LayoutScope.layoutBody() {}

    /** For the action buttons of your modal. See [primaryButton], [cancelButton] & [primaryAndCancelButtons]. */
    abstract fun LayoutScope.layoutButtons()

    override fun handleEscapeKeyPress() {
        if (requiresButtonPress) {
            return
        }

        super.handleEscapeKeyPress()
    }

    /** A [text] with some defaults for a modal title. */
    fun LayoutScope.title(text: State<String>, modifier: Modifier = Modifier) {
        text(
            text,
            Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(Color.BLACK).then(modifier),
        )
    }

    fun LayoutScope.title(text: String, modifier: Modifier = Modifier) = title(stateOf(text), modifier)

    /** A [wrappedText] with some defaults for a modal's description. Intended for use in [layoutBody]. */
    fun LayoutScope.description(
        text: String,
        textModifier: Modifier = Modifier,
        block: WrappedTextBuilder.() -> Unit = {},
    ) {
        wrappedText(
            text,
            textModifier = Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(Color.BLACK).then(textModifier),
            block = block,
        )
    }

    /**
     * [action] is a suspend function which is called on left-click. While the [action] is executing,
     * the button will be in a "disabled" state, where no click events will propagate.
     *
     * The [StyledButton.Style.disabledStyle] will be used while the [action] is being executed.
     *
     * NOTE: This will *not* close the modal on left-click, you must do that manually in your [action].
     */
    fun LayoutScope.primaryButton(
        text: String,
        modifier: Modifier = Modifier,
        style: StyledButton.Style = StyledButton.Style.BLUE,
        action: suspend () -> Unit,
    ) {
        styledButton(
            Modifier
                .width(91f)
                .tag(PrimaryAction)
                .then(modifier),
            style = style,
            action = action,
        ) { currentStyle ->
            text(text, Modifier.textStyle(currentStyle))
        }
    }

    fun LayoutScope.cancelButton(
        text: String,
        modifier: Modifier = Modifier,
        action: () -> Unit = ::close,
    ) {
        styledButton(
            Modifier
                .width(91f)
                .onLeftClick {
                    USound.playButtonPress()
                    action()
                }
                .focusable()
                .then(modifier),
        ) { style ->
            text(text, Modifier.textStyle(style))
        }
    }

    /**
     * An overload of [gg.essential.gui.common.styledButton].
     *
     * [action] is a suspend function which is called on left-click. While the [action] is executing,
     * the button will be in a "disabled" state, where no click events will propagate.
     *
     * The [StyledButton.Style.disabledStyle] will be used while the [action] is being executed.
     */
    fun LayoutScope.styledButton(
        modifier: Modifier = Modifier,
        style: State<StyledButton.Style> = stateOf(StyledButton.Style.GRAY),
        enableRetexturing: State<Boolean> = stateOf(false),
        action: suspend () -> Unit,
        content: LayoutScope.(style: State<MenuButton.Style>) -> Unit,
    ) {
        val disabled = mutableStateOf(false)

        styledButton(
            Modifier
                .onLeftClick {
                    USound.playButtonPress()

                    coroutineScope.launch {
                        disabled.set(true)
                        action()
                        disabled.set(false)
                    }
                }
                .focusable(disabled)
                .then(modifier),
            style,
            enableRetexturing,
            disabled,
            content
        )
    }

    /** See [styledButton], just an overload without any state to make it easier to call. */
    fun LayoutScope.styledButton(
        modifier: Modifier = Modifier,
        style: StyledButton.Style,
        enableRetexturing: Boolean = false,
        action: suspend () -> Unit,
        content: LayoutScope.(style: State<MenuButton.Style>) -> Unit,
    ) {
        styledButton(
            modifier,
            stateOf(style),
            stateOf(enableRetexturing),
            action,
            content,
        )
    }

    /** Indicates that this component is considered to be the [EssentialModal2]'s primary action. */
    data object PrimaryAction : Tag
}