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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.lazyHeight
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

class EssentialDropDown<T>(
    initialSelection: T,
    private val items: ListState<Option<T>>,
    var maxHeight: Float = Float.MAX_VALUE,
    val compact: State<Boolean> = stateOf(false),
    val disabled: State<Boolean> = stateOf(false),
) : UIBlock() {

    private val mutableExpandedState: MutableState<Boolean> = mutableStateOf(false)

    private val highlightedColor = EssentialPalette.BUTTON_HIGHLIGHT
    private val componentBackgroundColor = EssentialPalette.COMPONENT_BACKGROUND
    private val componentBackgroundHighlightColor = EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT

    private val mainButtonTextColor = disabled.map { if(it) EssentialPalette.TEXT_DISABLED else EssentialPalette.TEXT }
    private val mainButtonTextHoverColor = disabled.map { if(it) EssentialPalette.TEXT_DISABLED else EssentialPalette.TEXT_HIGHLIGHT }


    private val dropdownColorState = stateBy {
        when {
            disabled() -> EssentialPalette.COMPONENT_BACKGROUND
            mutableExpandedState() -> highlightedColor
            else -> EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT
        }
    }.toV1(this@EssentialDropDown)
    private val dropdownHoverColorState = disabled.map { if(it) EssentialPalette.COMPONENT_BACKGROUND else highlightedColor }.toV1(this@EssentialDropDown)

    private val optionTextPadding = 5f
    private val iconContainerWidth = 15f
    private val maxItemWidthState = stateBy { items().maxOfOrNull { it.textState().width() + 2 * optionTextPadding + iconContainerWidth } ?: 50f }

    /** Public States **/
    val selectedOption: MutableState<Option<T>> = mutableStateOf(items.get().first { it.value == initialSelection })
    val expandedState: State<Boolean> = mutableExpandedState

    init {

        fun LayoutScope.option(option: Option<T>) {
            val colorModifier = Modifier
                .color(option.color)
                .hoverColor(option.hoveredColor)
                .shadow(option.shadowColor)
                .hoverShadow(option.hoveredShadowColor)

            row(Modifier.height(15f).fillWidth().color(componentBackgroundColor).hoverColor(componentBackgroundHighlightColor).hoverScope(), Arrangement.SpaceBetween) {
                box(Modifier.childBasedWidth(optionTextPadding)) {
                    text(option.textState.toV1(this@EssentialDropDown), colorModifier, centeringContainsShadow = false)
                }
                box(Modifier.width(iconContainerWidth)) {
                    if_(selectedOption.map { it == option }) {
                        icon(EssentialPalette.CHECKMARK_7X5, colorModifier then Modifier.alignHorizontal(Alignment.Start(3f)))
                    }
                }
            }.onLeftClick {
                if(disabled.get())
                    return@onLeftClick

                USound.playButtonPress()
                it.stopPropagation()
                select(option)
            }

        }

        fun Modifier.customWidth() = this then BasicWidthModifier { basicWidthConstraint { maxItemWidthState.get() } }

        fun Modifier.maxSiblingHeight() = this then BasicHeightModifier {
            basicHeightConstraint { it.parent.children.maxOfOrNull { child -> if (child === it) 0f else child.getHeight() } ?: 1f }
        }

        fun Modifier.limitHeight() = this then {
            val originalHeightConstraint = constraints.height

            val distanceToWindowBorder = lazyHeight {
                basicHeightConstraint {
                    // To get the remaining height we have available for the scrollbar we subtract:
                    // - the position of the expandedBlock (this@EssentialDropDown.getBottom())
                    // - 9f for padding (4f + 5f)
                    //   - 4f is from the outline around the scrollbar (of which we are limiting the height)
                    //   - 5f is the actual padding to the window
                    Window.of(this).getHeight() - this@EssentialDropDown.getBottom() - 9f
                }
            }

            constraints.height = originalHeightConstraint.coerceAtMost(distanceToWindowBorder).coerceAtMost(maxHeight.pixels)

            return@then { constraints.height = originalHeightConstraint }
        }

        val arrowIconState = mutableExpandedState.map {
            if (it) {
                EssentialPalette.ARROW_UP_7X4
            } else {
                EssentialPalette.ARROW_DOWN_7X4
            }
        }.toV1(this@EssentialDropDown)

        componentName = "dropdown"

        this.layout(Modifier.height(17f).whenTrue(compact, Modifier.widthAspect(1f), Modifier.customWidth())) {
            column(Modifier.fillParent(), Arrangement.spacedBy(0f, FloatPosition.START), Alignment.Start) {
                box(Modifier.fillParent().color(dropdownColorState).hoverColor(dropdownHoverColorState).shadow().hoverScope()) {
                    if_(compact) {
                        icon(
                            EssentialPalette.FILTER_6X5,
                            Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT).shadow()
                        )
                    } `else` {
                        text(
                            stateBy { selectedOption().textState() }.toV1(this@EssentialDropDown),
                            Modifier
                                .alignHorizontal(Alignment.Start(7f))
                                .color(mainButtonTextColor).hoverColor(mainButtonTextHoverColor)
                                .shadow(EssentialPalette.TEXT_SHADOW),
                            centeringContainsShadow = false,
                        )
                        icon(arrowIconState, Modifier.alignVertical(Alignment.Center(true)).alignHorizontal(Alignment.End(7f))
                            .color(mainButtonTextColor).hoverColor(mainButtonTextHoverColor))
                    }
                }.onLeftClick { event ->
                    if (disabled.get())
                        return@onLeftClick

                    USound.playButtonPress()
                    event.stopPropagation()

                    if (mutableExpandedState.get()) {
                        collapse()
                    } else {
                        expand()
                    }
                }

                if_(compact) {
                    spacer(height = 2f)
                }

                val heightConstraintState = mutableExpandedState.map {
                    if (it) {
                        { ChildBasedMaxSizeConstraint() + 4.pixels }
                    } else {
                        { 0.pixels }
                    }
                }

                val heightModifierState = stateBy {
                    if (compact()) {
                        BasicHeightModifier(heightConstraintState())
                    } else {
                        Modifier.animateHeight(heightConstraintState, 0.25f)
                    }
                }

                floatingBox(
                    Modifier.whenTrue(compact, Modifier.customWidth(), Modifier.fillWidth())
                        .color(highlightedColor).shadow().effect { ScissorEffect() }
                        .then(heightModifierState)
                ) {
                    val scrollBar: UIComponent
                    val scrollComponent = scrollable(Modifier.fillWidth(padding = 2f).limitHeight(), vertical = true) {
                        column(
                            Modifier.childBasedHeight(3f).fillWidth().color(componentBackgroundColor),
                            Arrangement.spacedBy(0f, FloatPosition.CENTER)
                        ) {
                            forEach(items) {
                                option(it)
                            }
                        }
                    }
                    box(Modifier.maxSiblingHeight().width(2f).alignHorizontal(Alignment.End)) {
                        scrollBar = box(Modifier.fillWidth().color(EssentialPalette.TEXT_DISABLED))
                    }
                    scrollComponent.setVerticalScrollBarComponent(scrollBar, true)
                }
            }
        }
    }

    fun select(option: Option<T>) {
        if (items.get().contains(option)) {
            selectedOption.set(option)
            collapse()
        }
    }

    fun expand() {
        mutableExpandedState.set(true)
    }

    fun collapse() {
        mutableExpandedState.set(false)
    }

    class Option<T>(
        val textState: State<String>,
        val value: T,
        val color: Color = EssentialPalette.TEXT,
        val hoveredColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
        val shadowColor: Color = EssentialPalette.BLACK,
        val hoveredShadowColor: Color = shadowColor,
    ) {
        constructor(
            text: String,
            value: T,
            color: Color = EssentialPalette.TEXT,
            hoveredColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
            shadowColor: Color = EssentialPalette.BLACK,
            hoveredShadowColor: Color = shadowColor,
        ) : this(
            stateOf(text),
            value,
            color,
            hoveredColor,
            shadowColor,
            hoveredShadowColor
        )
    }

}
