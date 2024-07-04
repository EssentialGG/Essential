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
package gg.essential.gui.modals

import gg.essential.Essential
import gg.essential.data.OnboardingData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.outline.GuiScaleOffsetOutline
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import gg.essential.gui.elementa.state.v2.await
import gg.essential.gui.overlay.ModalManager
import gg.essential.network.connectionmanager.ConnectionManager
import gg.essential.universal.ChatColor
import gg.essential.universal.UMinecraft
import gg.essential.universal.USound
import gg.essential.util.AutoUpdate
import gg.essential.util.GuiUtil
import gg.essential.util.findChildOfTypeOrNull
import gg.essential.util.times
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.launch
import java.net.URI

class TOSModal(
    modalManager: ModalManager,
    unprompted: Boolean = false,
    requiresAuth: Boolean = false,
    confirmAction: Modal.() -> Unit,
    cancelAction: () -> Unit = {},
) : ConfirmDenyModal(modalManager, unprompted) {

    //#if MC<=11802
    private val forceUnicodeEnabled = UMinecraft.getSettings().forceUnicodeFont
    //#else
    //$$ private val forceUnicodeEnabled = UMinecraft.getSettings().forceUnicodeFont.value
    //#endif

    private val ageCheckboxOutlineColor = EssentialPalette.ACCENT_BLUE.state().map { it }
    private val tosCheckboxOutlineColor = EssentialPalette.ACCENT_BLUE.state().map { it }

    private val confirmContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = SiblingConstraint()
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf customContent

    private val ageConfirmContainer by UIContainer().constrain {
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    }.onLeftClick { findChildOfTypeOrNull<Checkbox>()?.toggle() } childOf confirmContainer

    private val ageCheckbox by Checkbox(initialValue = false, checkmarkScaleOffset = -1f, playClickSound = false) {
        USound.playExpSound()
    }.constrain {
        y = CenterConstraint() + if (forceUnicodeEnabled) 1.pixel else 0.pixels
        width = AspectConstraint()
        height = 9.pixels * GuiScaleOffsetConstraint(-1f)
    }.effect(
        GuiScaleOffsetOutline(
            offset = -1f,
            color = ageCheckboxOutlineColor,
        )
    ) childOf ageConfirmContainer

    private val ageText by EssentialUIText(
        "I'm 13+ years old and have parental consent, or I'm 18+ years old.",
        shadow = false,
    ).constrain {
        x = SiblingConstraint(4f)
        y = CenterConstraint()
        textScale = guiScaleUnicodeOffset()
        color = EssentialPalette.TEXT.toConstraint()
    } childOf ageConfirmContainer

    private val tosConfirmContainer by UIContainer().constrain {
        y = SiblingConstraint(4f)
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    }.onLeftClick { findChildOfTypeOrNull<Checkbox>()?.toggle() } childOf confirmContainer

    private val tosCheckbox by Checkbox(initialValue = false, checkmarkScaleOffset = -1f, playClickSound = false) {
        USound.playExpSound()
    }.constrain {
        y = CenterConstraint() + if (forceUnicodeEnabled) 1.pixel else 0.pixels
        width = AspectConstraint()
        height = 100.percent boundTo ageCheckbox
    }.effect(
        GuiScaleOffsetOutline(
            offset = -1f,
            color = tosCheckboxOutlineColor,
        )
    ) childOf tosConfirmContainer

    // Hack to get highlightable links and smaller text since MarkdownComponent currently doesn't support them or textScale constraint
    // TODO: Improve this when possible
    private val tosText1 by EssentialUIText("I accept the ", shadow = false).constrain {
        x = SiblingConstraint(4f)
        y = CenterConstraint()
        textScale = guiScaleUnicodeOffset()
        color = EssentialPalette.TEXT.toConstraint()
    } childOf tosConfirmContainer

    private val tosText2 by EssentialUIText("${ChatColor.UNDERLINE}Terms of Service", shadow = false).constrain {
        x = SiblingConstraint()
        y = 0.pixels boundTo tosText1
        textScale = guiScaleUnicodeOffset()
        color = EssentialPalette.TEXT.toConstraint()
    }.onMouseEnter {
        setColor(EssentialPalette.TEXT_HIGHLIGHT)
    }.onMouseLeave {
        setColor(EssentialPalette.TEXT)
    }.onLeftClick {
        GuiUtil.pushModal { OpenLinkModal(it, URI("https://essential.gg/terms-of-use")) }
        it.stopPropagation()
    } childOf tosConfirmContainer

    private val tosText3 by EssentialUIText(" and the Essential ", shadow = false).constrain {
        x = SiblingConstraint()
        y = 0.pixels boundTo tosText1
        textScale = guiScaleUnicodeOffset()
        color = EssentialPalette.TEXT.toConstraint()
    } childOf tosConfirmContainer

    private val tosText4 by EssentialUIText("${ChatColor.UNDERLINE}Privacy Policy.", shadow = false).constrain {
        x = SiblingConstraint()
        y = 0.pixels boundTo tosText1
        textScale = guiScaleUnicodeOffset()
        color = EssentialPalette.TEXT.toConstraint()
    }.onMouseEnter {
        setColor(EssentialPalette.TEXT_HIGHLIGHT)
    }.onMouseLeave {
        setColor(EssentialPalette.TEXT)
    }.onLeftClick {
        GuiUtil.pushModal { OpenLinkModal(it, URI("https://essential.gg/privacy-policy")) }
        it.stopPropagation()
    } childOf tosConfirmContainer

    init {
        configure {
            if (unprompted) {
                contentText = "To use Essential's features, you must first accept our Terms of Service."
                cancelButtonText = "Deny"
                primaryButtonText = "Accept"
                cancelButton.rebindStyle(BasicState(MenuButton.RED), BasicState(MenuButton.LIGHT_RED))
            } else {
                contentText = "An Essential feature you are trying\n to use requires you to accept our\n Terms of Service."
                cancelButtonText = "Back"
            }

            // Resize for larger modal
            content.setWidth(
                (componentWidthConstraint(confirmContainer) + 4.pixels).coerceAtLeast(
                    componentWidthConstraint(buttonContainer) + 17.pixels
                )
            )
        }

        // Bottom padding
        Spacer(height = 18f) childOf customContent

        ageCheckbox.isChecked.zip(tosCheckbox.isChecked).onSetValueAndNow { (ageChecked, tosChecked) ->
            primaryButtonAction =
                if (!ageChecked || !tosChecked) {
                    {   // Highlight checkboxes in red if either is left unchecked
                        ageCheckboxOutlineColor.rebind(ageCheckbox.isChecked.map { if (it) EssentialPalette.ACCENT_BLUE else EssentialPalette.ESSENTIAL_RED })
                        tosCheckboxOutlineColor.rebind(tosCheckbox.isChecked.map { if (it) EssentialPalette.ACCENT_BLUE else EssentialPalette.ESSENTIAL_RED })
                    }
                } else {
                    {
                        OnboardingData.setAcceptedTos()

                        if (requiresAuth && !Essential.getInstance().connectionManager.isAuthenticated) {
                            primaryButtonText = "Connecting..."
                            requiresButtonPress = true
                            primaryActionButton.rebindEnabled(false.state())
                            cancelButton.rebindEnabled(false.state())
                            val connectionManager = Essential.getInstance().connectionManager

                            coroutineScope.launch {
                                val status = connectionManager.connectionStatus.await { it != null && it != ConnectionManager.Status.NO_TOS }
                                val modal = when {
                                    connectionManager.outdated -> AutoUpdate.createUpdateModal(modalManager)
                                    status == ConnectionManager.Status.MOJANG_UNAUTHORIZED -> AccountNotValidModal(modalManager, confirmAction)
                                    status != ConnectionManager.Status.SUCCESS -> NotAuthenticatedModal(modalManager, confirmAction)
                                    else -> null
                                }

                                if (modal != null) {
                                    replaceWith(modal)
                                } else {
                                    confirmAction.invoke(this@TOSModal)
                                    replaceWith(null)
                                }
                            }
                        } else {
                            confirmAction.invoke(this)
                            replaceWith(null)
                        }
                    }
                }
        }

        onCancel {
            if (unprompted) {
                OnboardingData.setDeniedTos()
            }
            cancelAction.invoke()
        }
    }

    private fun guiScaleUnicodeOffset(): GuiScaleOffsetConstraint {
        return GuiScaleOffsetConstraint(if (forceUnicodeEnabled) 0f else -1f)
    }
}
