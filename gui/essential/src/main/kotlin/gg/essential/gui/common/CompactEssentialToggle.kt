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
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.*
import gg.essential.universal.USound
import gg.essential.util.centered
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.pollingState
import gg.essential.gui.util.stateBy
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

abstract class EssentialToggle(
    private val enabled: State<Boolean>,
    private val boxOffset: Int
) : UIBlock() {
    protected val switchBox by UIBlock().constrain {
        y = CenterConstraint()
        width = AspectConstraint()
    } childOf this

    init {
        onLeftClick {
            USound.playButtonPress()
            enabled.set { !it }
        }

        enabled.onSetValueAndNow {
            val xConstraint = boxOffset.pixel(alignOpposite = it)
            // Null during init
            if (Window.ofOrNull(this@EssentialToggle) != null) {
                switchBox.animate {
                    setXAnimation(Animations.OUT_EXP, 0.25f, xConstraint)
                }
            } else {
                switchBox.setX(xConstraint)
            }
        }
    }
}

class FullEssentialToggle(
    enabled: State<Boolean>,
    backgroundColor: Color,
) : EssentialToggle(enabled, 1) {
    // This component is used in Vigilance, and we use the property here
    // to avoid a divergence in the toggle implementations
    private val showToggleIndicators = pollingState {
        System.getProperty("essential.hideSwitchIndicators") != "true"
    }

    private val accentColor = enabled.map {
            if (it) {
                EssentialPalette.ACCENT_BLUE
            } else {
                EssentialPalette.TEXT
            }
        }

    private val onIndicator by UIContainer().constrain {
        height = 100.percent
        width = 50.percent
    }.addChild {
        EssentialPalette.TOGGLE_ON.withColor(backgroundColor).create().centered()
    }.bindParent(this, showToggleIndicators and enabled, index = 0)

    private val offIndicator by UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        height = 100.percent
        width = 50.percent
    }.addChild {
        EssentialPalette.TOGGLE_OFF
            .create()
            .constrain {
                color = EssentialPalette.TEXT_MID_GRAY.toConstraint()
            }
            .centered()
    }.bindParent(this, showToggleIndicators and !enabled, index = 0)

    init {
        constrain {
            width = 20.pixels
            height = 11.pixels
        }

        setColor(accentColor.toConstraint())

        switchBox.constrain {
            height = 100.percent - 2.pixels
            color = hoveredState().map { hovered ->
                    if (hovered) {
                        EssentialPalette.BUTTON
                    } else {
                        backgroundColor
                    }
                }.toConstraint()
        }

    }
}

fun LayoutScope.compactFullEssentialToggle(
    enabled: State<Boolean>,
    modifier: Modifier = Modifier,
    offColor: State<Color> = BasicState(EssentialPalette.TEXT_MID_GRAY),
    onColor: State<Color> = BasicState(EssentialPalette.GREEN),
    shadowColor: State<Color?> = BasicState(EssentialPalette.BLACK),
) {
    val color: State<Color> = stateBy { if (enabled()) onColor() else offColor() }
    val coloredModifier = Modifier.color(color).hoverColor(color.map { it.brighter() }).then(shadowColor.map { if (it != null) Modifier.shadow(it) else Modifier })

    column(Modifier.width(10f).height(6f).then(modifier)) {
        box(coloredModifier.fillWidth().height(1f))
        row(Modifier.fillWidth().fillRemainingHeight()) {
            box(coloredModifier.width(1f).fillHeight())
            row(Modifier.fillRemainingWidth().fillHeight(), Arrangement.SpaceBetween) {
                box(coloredModifier.fillHeight().animateWidth(enabled.toV2().map {{ if (it) 50.percent else 0.percent }}, 0.25f))
                box(coloredModifier.fillHeight().animateWidth(enabled.toV2().map {{ if (it) 0.percent else 50.percent }}, 0.25f))
            }
            box(coloredModifier.width(1f).fillHeight())
        }
        box(coloredModifier.fillWidth().height(1f))
    }.onLeftClick { click ->
        USound.playButtonPress()
        enabled.set { !it }
        click.stopPropagation()
    }
}
