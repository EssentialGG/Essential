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
package gg.essential.gui.common

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.image.ImageFactory
import gg.essential.universal.USound
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class IconButton(
    imageFactory: State<ImageFactory>,
    tooltipText: State<String>,
    enabled: State<Boolean>,
    buttonText: State<String>,
    iconShadow: State<Boolean>,
    textShadow: State<Boolean>,
    tooltipBelowComponent: Boolean,
    buttonShadow: Boolean = true,
) : UIBlock() {

    private val hovered = hoveredState()

    private val iconState = imageFactory.map { it }
    private val iconShadowState = iconShadow.map { it }
    private val iconShadowColor = BasicState(EssentialPalette.TEXT_SHADOW).map { it }
    private val tooltipState = tooltipText.map { it }
    private val enabledState = enabled.map { it }
    private val buttonTextState = buttonText.map { it }
    private val textShadowState = textShadow.map { it }
    private val textShadowColor = BasicState(EssentialPalette.TEXT_SHADOW).map { it }
    private val textColor = EssentialPalette.getTextColor(hovered, enabledState).map { it }
    private val layoutState = BasicState(Layout.ICON_FIRST)
    private val dimension: State<Dimension> = BasicState(Dimension.FitWithPadding(10f, 10f))

    constructor(
        imageFactory: ImageFactory,
        buttonText: String = "",
        tooltipText: String = "",
        iconShadow: Boolean = true,
        textShadow: Boolean = true,
        tooltipBelowComponent: Boolean = true,
        buttonShadow: Boolean = true,
    ) : this(
        BasicState(imageFactory),
        BasicState(tooltipText),
        BasicState(true),
        BasicState(buttonText),
        BasicState(iconShadow),
        BasicState(textShadow),
        tooltipBelowComponent,
        buttonShadow
    )

    constructor(
        imageFactory: ImageFactory,
        buttonText: State<String>
    ) : this(
        BasicState(imageFactory),
        BasicState(""),
        BasicState(true),
        buttonText,
        BasicState(true),
        BasicState(true),
        true,
        true
    )

    private val content by UIContainer().centered().constrain {
        width = ChildBasedSizeConstraint()
        height = ChildBasedMaxSizeConstraint()
    } childOf this


    private val icon by ShadowIcon(iconState, iconShadowState).constrain {
        //x constraint set in init
        y = CenterConstraint()
    }.rebindPrimaryColor(EssentialPalette.getTextColor(hovered, enabledState)).rebindShadowColor(iconShadowColor) childOf content

    private val tooltip = EssentialTooltip(
        this,
        position = if (tooltipBelowComponent) EssentialTooltip.Position.BELOW else EssentialTooltip.Position.ABOVE,
    ).constrain {
        x = CenterConstraint() boundTo this@IconButton coerceAtMost 4.pixels(alignOpposite = true)
        y = SiblingConstraint(5f, alignOpposite = !tooltipBelowComponent) boundTo this@IconButton
    }.bindVisibility(hovered and !tooltipState.empty()) as EssentialTooltip

    private val buttonText by EssentialUIText(centeringContainsShadow = false).bindText(buttonTextState)
        .bindShadow(textShadowState)
        .bindShadowColor(textShadowColor.map { it })
        .setColor(textColor.toConstraint())
        .bindConstraints(layoutState) {
            y = CenterConstraint()
            x = when (it) {
                Layout.ICON_FIRST -> SiblingConstraint(5f)
                Layout.TEXT_FIRST -> 0.pixels
            }
        }.bindParent(content, !buttonTextState.empty())

    init {
        setColor(EssentialPalette.getButtonColor(hovered, enabledState).toConstraint())

        bindConstraints(dimension) {
            when (it) {
                is Dimension.FitWithPadding -> {
                    width = ChildBasedSizeConstraint() + it.widthPadding.pixels
                    height = ChildBasedSizeConstraint() + it.heightPadding.pixels
                }
                is Dimension.Fixed -> {
                    width = it.width.pixels
                    height = it.height.pixels
                }
            }
        }

        onLeftClick {
            if (enabledState.get()) {
                USound.playButtonPress()
                it.stopPropagation()
            } else {
                // If the button is disabled we don't want events to be passed to parent components or sibling listeners
                it.stopImmediatePropagation()
            }
        }

        layoutState.zip(buttonTextState.empty()).onSetValueAndNow { (layout, emptyText) ->
            icon.setX(
                if (emptyText) {
                    CenterConstraint()
                } else {
                    when (layout) {
                        Layout.ICON_FIRST -> 0.pixels
                        Layout.TEXT_FIRST -> SiblingConstraint(6f) boundTo this@IconButton.buttonText
                    }
                }
            )
        }

        tooltip.bindLine(tooltipState)

        if (buttonShadow) {
            effect(ShadowEffect(Color.BLACK))
        }
    }

    fun rebindIcon(imageFactory: State<ImageFactory>): IconButton {
        iconState.rebind(imageFactory)
        return this
    }

    fun rebindTooltipText(tooltipText: State<String>): IconButton {
        tooltipState.rebind(tooltipText)
        return this
    }

    fun rebindEnabled(enabled: State<Boolean>): IconButton {
        enabledState.rebind(enabled)
        return this
    }

    fun rebindIconColor(color: State<Color>): IconButton {
        icon.rebindPrimaryColor(color)
        return this
    }

    fun rebindTextColor(color: State<Color>): IconButton {
        textColor.rebind(color)
        return this
    }

    fun setLayout(layout: Layout): IconButton {
        layoutState.set(layout)
        return this
    }

    fun setDimension(dimension: Dimension): IconButton {
        this.dimension.set(dimension)
        return this
    }


    fun onActiveClick(action: () -> Unit): IconButton {
        onLeftClick {
            if (enabledState.get()) {
                action()
            }
        }
        return this
    }

    sealed class Dimension {
        data class FitWithPadding(val widthPadding: Float, val heightPadding: Float) : Dimension()
        data class Fixed(val width: Float, val height: Float) : Dimension()
    }

    enum class Layout {

        ICON_FIRST,
        TEXT_FIRST,

    }

}