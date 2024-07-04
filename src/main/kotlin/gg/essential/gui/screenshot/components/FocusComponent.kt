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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.IconButton
import gg.essential.gui.common.and
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.or
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.State as StateV2
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.flatten
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.layoutdsl.row
import gg.essential.universal.USound
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick


open class FocusComponent(
    private val screenshotBrowser: ScreenshotBrowser,
    private val focusType: FocusType
) : ScreenshotView(View.FOCUS, screenshotBrowser) {

    final override val active: State<Boolean> = super.active and screenshotBrowser.focusType.map { it == focusType }
    private val isFocusView = screenshotBrowser.focusType.map { it == FocusType.VIEW }

    val previewing: ScreenshotProperties
        get() = screenshotBrowser.focusing.get()!!

    val isMainScreenshotErroredSource: MutableState<StateV2<Boolean>> = mutableStateOf(stateOf(false))

    val isMainScreenshotErrored = isMainScreenshotErroredSource.flatten()

    val providerManager = screenshotBrowser.providerManager

    val buttonSize = 17f

    private val backButton by IconButton(EssentialPalette.ARROW_LEFT_4X7, "Back")
        .onLeftClick {
            USound.playButtonPress()
            onBackButtonPressed()
        }

    protected val delete by IconButton(EssentialPalette.TRASH_9X, tooltipText = "Delete")
        .setDimension(IconButton.Dimension.Fixed(buttonSize, buttonSize))

    // Bound to states in init of child objects
    protected val favorite by IconButton(EssentialPalette.HEART_EMPTY_9X, tooltipText = "Favorite")
        .setDimension(IconButton.Dimension.Fixed(buttonSize, buttonSize))

    protected val share by ShareButton(screenshotBrowser)
        .setDimension(IconButton.Dimension.Fixed(buttonSize, buttonSize))

    protected val properties by IconButton(EssentialPalette.PROPERTIES_7X5, tooltipText = "Properties")
        .setDimension(IconButton.Dimension.Fixed(buttonSize, buttonSize))
        .onLeftClick {
            screenshotBrowser.displayPropertiesModal(previewing)
        }

    protected val edit by IconButton(EssentialPalette.EDIT_10X7, "Edit")
        .setDimension(IconButton.Dimension.Fixed(44f, buttonSize))
        .onLeftClick {
            screenshotBrowser.openEditor()
        }

    protected val container by UIContainer().constrain {
        width = 100.percent
        height = 100.percent
    } childOf this

    val time = EssentialUIText().constrain {
        x = CenterConstraint()
        y = CenterPixelConstraint()
    }

    init {
        screenshotBrowser.titleBar.layoutAsBox {
            if_(active) {
                box(Modifier.fillWidth(padding = 10f).fillHeight()) {
                    backButton(Modifier.alignHorizontal(Alignment.Start))
                    time(Modifier.alignHorizontal(Alignment.Center))
                    row(Modifier.alignHorizontal(Alignment.End), Arrangement.spacedBy(10f)) {
                        if_(isMainScreenshotErrored, cache = false) {
                            row(Arrangement.spacedBy(3f)) {
                                properties()
                                delete()
                            }
                        } `else` {
                            if_(isFocusView) {
                                row(Arrangement.spacedBy(3f)) {
                                    edit()
                                    properties()
                                }
                            }

                            row(Arrangement.spacedBy(3f)) {
                                if_(isFocusView or BasicState(true)) {
                                    share()
                                }
                                if_(isFocusView) {
                                    favorite()
                                    delete()
                                }
                            }
                        }
                    }
                }
            }
        }

        constrain {
            width = 100.percent
            height = 100.percent
        }

        bindParent(screenshotBrowser.content, active)
    }

    /**
     * Opens the focus around the specified index. Exits if index is -1
     */
    fun focus(index: Int) {
        if (index == -1) {
            screenshotBrowser.closeFocus()
            return
        }
        val properties = providerManager.propertyMap[providerManager.currentPaths[index]]
        if (properties != null) {
            screenshotBrowser.changeFocusedComponent(properties)
        }
    }

    fun deleteCurrentFocus(closeAfter: Boolean) {
        val focusing = screenshotBrowser.focusing.get()
        if (focusing != null) {
            val previewIndex = screenshotBrowser.providerManager.currentPaths.indexOf(focusing.id)
            val size = providerManager.currentPaths.size

            val nextIndex = if (previewIndex < size - 1) previewIndex else previewIndex - 1
            providerManager.handleDelete(focusing) {
                focus(nextIndex)
                if (closeAfter) {
                    screenshotBrowser.closeFocus()
                }
            }
        }
    }

    fun applyFavoriteState(favState: State<Boolean>) {
        favorite.rebindIcon(favState.map { favorite ->
            if (favorite) {
                EssentialPalette.HEART_FILLED_9X
            } else {
                EssentialPalette.HEART_EMPTY_9X
            }
        }).rebindTooltipText(screenshotBrowser.stateManager.mapFavoriteText(favState))
            .rebindIconColor(
                favState.zip(EssentialPalette.getTextColor(favorite.hoveredState()))
                    .map { (favorite, hoverColor) ->
                        if (favorite) {
                            EssentialPalette.TEXT_RED
                        } else {
                            hoverColor
                        }
                    })
    }

    /**
     * Called on close to reset states and save any needed states
     */
    open fun onClose() {}

    /**
     * Fired when the back button is pressed
     */
    open fun onBackButtonPressed() {
        screenshotBrowser.closeFocus()
    }
}

