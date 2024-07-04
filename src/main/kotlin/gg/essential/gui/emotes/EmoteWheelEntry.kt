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
package gg.essential.gui.emotes

import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.MousePositionConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.model.BedrockModel
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UScreen
import gg.essential.universal.wrappers.UPlayer
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Polygon
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.layoutAsBox
import gg.essential.gui.elementa.state.v2.State as StateV2

class EmoteWheelEntry(
    val emoteModel: StateV2<BedrockModel?>?,
    val index: Int,
    window: Window,
    debug: BasicState<Boolean>,
) : UIContainer() {
    private val hoveredState = hoveredState()

    // Static values
    private val padding = 3f
    private val displayBlockSize = 57f
    private val corner = index % 2 == 1

    // Debug stuff
    private val debugColor = hoveredState.map { (if (it) Color.RED else if (corner) Color.PINK else Color.CYAN).withAlpha(150) }
    private val debugHitbox by EssentialShape(debugColor, (index == 3 || index == 7)).bindParent(this, debug, index = 0)

    private val displayBlockColor =
        hoveredState.map { if (it) EssentialPalette.DARK_TRANSPARENT_BACKGROUND_HIGHLIGHTED else EssentialPalette.DARK_TRANSPARENT_BACKGROUND }

    val displayBlock by UIBlock(displayBlockColor).constrain {
        if (corner) {
            x = (padding + displayBlockSize / 2).pixels(alignOpposite = (index == 1 || index == 7))
            y = (padding + displayBlockSize / 2).pixels(alignOpposite = (index == 1 || index == 3))
        } else {
            x = if (index == 2 || index == 8) CenterConstraint() else (padding + displayBlockSize / 4).pixels(alignOpposite = (index == 4))
            y = if (index == 4 || index == 6) CenterConstraint() else (padding + displayBlockSize / 4).pixels(alignOpposite = (index == 2))
        }
        width = displayBlockSize.pixels
        height = AspectConstraint()
    }.bindEffect(OutlineEffect(EssentialPalette.MESSAGE_SENT, 1f), hoveredState) childOf this

    private val hitboxPoints =
        if (corner) {
            listOf(
                UIPoint(
                    (if (index == 1 || index == 7) 0.percent else 100.percent) boundTo window,
                    (if (index == 1 || index == 3) 0.percent else 100.percent) boundTo window,
                ),
                UIPoint(
                    (if (index == 1 || index == 7) 0.percent else 100.percent) boundTo window,
                    (if (index == 1 || index == 3) 34.percent else 65.percent) boundTo window,
                ),
                UIPoint(
                    SiblingConstraint(padding, alignOpposite = index == 1 || index == 7) boundTo displayBlock,
                    SiblingConstraint(padding, alignOpposite = index == 7 || index == 9) boundTo displayBlock,
                ),
                UIPoint(
                    50.percent boundTo displayBlock,
                    if (index == 1 || index == 3) 100.percent else 0.percent,
                ),
                UIPoint(
                    SiblingConstraint(padding + displayBlockSize / 4, alignOpposite = index == 3 || index == 9) boundTo displayBlock,
                    SiblingConstraint(padding + displayBlockSize / 4, alignOpposite = index == 7 || index == 9) boundTo displayBlock,
                ),
                UIPoint(
                    if (index == 1 || index == 7) 100.percent else 0.percent,
                    50.percent boundTo displayBlock,
                ),
                UIPoint(
                    SiblingConstraint(padding, alignOpposite = index == 3 || index == 9) boundTo displayBlock,
                    SiblingConstraint(padding, alignOpposite = index == 1 || index == 3) boundTo displayBlock,
                ),
                UIPoint(
                    (if (index == 1 || index == 7) 38.percent else 62.percent) boundTo window,
                    (if (index == 1 || index == 3) 0.percent else 100.percent) boundTo window,
                ),
            )
        } else {
            listOf(
                UIPoint(
                    if (index == 2 || index == 4) 0.percent else 100.percent,
                    if (index == 2 || index == 6) 0.percent else 100.percent,
                ),
                UIPoint(
                    SiblingConstraint(padding, alignOpposite = index == 2 || index == 4) boundTo displayBlock,
                    SiblingConstraint(padding, alignOpposite = index == 2 || index == 6) boundTo displayBlock,
                ),
                UIPoint(
                    SiblingConstraint(padding / 2, alignOpposite = index == 2 || index == 6) boundTo displayBlock,
                    SiblingConstraint(padding / 2, alignOpposite = index == 6 || index == 8) boundTo displayBlock,
                ),
                UIPoint(
                    if (index == 2 || index == 8) 50.percent else 0.pixels(alignOpposite = index == 4),
                    if (index == 4 || index == 6) 50.percent else 0.pixels(alignOpposite = index == 2),
                ),
                UIPoint(
                    SiblingConstraint(padding / 2, alignOpposite = index == 6 || index == 8) boundTo displayBlock,
                    SiblingConstraint(padding / 2, alignOpposite = index == 4 || index == 8) boundTo displayBlock,
                ),
                UIPoint(
                    SiblingConstraint(padding, alignOpposite = index == 4 || index == 8) boundTo displayBlock,
                    SiblingConstraint(padding, alignOpposite = index == 2 || index == 4) boundTo displayBlock,
                ),
                UIPoint(
                    if (index == 2 || index == 6) 100.percent else 0.percent,
                    if (index == 6 || index == 8) 100.percent else 0.percent,
                ),
            )
        }

    init {
        constrain {
            x = if (index == 2 || index == 8) CenterConstraint() else 0.pixels(alignOpposite = index == 3 || index == 9 || index == 6)
            y = if (index == 4 || index == 6) CenterConstraint() else 0.pixels(alignOpposite = index == 8 || index == 7 || index == 9)
            width = if (index == 2 || index == 8) 26.percent else 50.percent - padding.pixels - (if (corner) 0 else displayBlockSize / 4).pixels
            height = if (index == 4 || index == 6) 35.percent else 50.percent - padding.pixels - (if (corner) 0 else displayBlockSize / 4).pixels
        }

        hitboxPoints.forEach { it childOf this }

        if (emoteModel != null) {
            val tooltipModifier = Modifier.hoverTooltip(
                emoteModel.map { it?.cosmetic?.getDisplayName("en_us") ?: it?.cosmetic?.id ?: "Loading..." },
                Modifier.color(EssentialPalette.MESSAGE_SENT),
                EssentialTooltip.Position.RIGHT,
                notchSize = 0
            ) {
                constrain {
                    x = MousePositionConstraint() + 10.pixels
                    y = MousePositionConstraint() - 15.pixels
                }
            }

            displayBlock.layoutAsBox(Modifier.hoverScope(hoveredState).then(tooltipModifier)) {
                ifNotNull(emoteModel) { model ->
                    CosmeticPreview(model.cosmetic)(Modifier.fillWidth(0.8f).heightAspect(1f))
                }
            }
        }

        // We use window focus here to know which emote was last hovered when the wheel is closed
        hoveredState.onSetValue { hovered ->
            if (hovered) {
                grabWindowFocus()
            } else {
                releaseWindowFocus()
            }
        }

        onLeftClick {
            emoteModel?.getUntracked()?.let { emote ->
                if (EmoteWheel.canEmote(UPlayer.getPlayer()!!)) {
                    EmoteWheel.equipEmote(emote)
                    EmoteWheel.emoteClicked = true
                    UScreen.displayScreen(null)
                }
            }
        }

        debug.onSetValueAndNow { debug ->
            if (debug && debugHitbox.getVertices().isEmpty()) {
                debugHitbox.addVertices(*hitboxPoints.toTypedArray())
            }
        }
    }

    // Create a Polygon from the hitboxPoints to check if x,y is inside polygon
    override fun isPointInside(x: Float, y: Float): Boolean {
        val hitbox = Polygon()
        hitboxPoints.forEach { hitbox.addPoint(it.absoluteX.toInt(), it.absoluteY.toInt()) }
        return hitbox.contains(x.toInt(), y.toInt())
    }
}

// For visually debugging hitboxes. Not super accurate but gives a general idea.
class EssentialShape(colorState: State<Color>, private val clockwise: Boolean = false) : UIShape() {

    init { colorState.onSetValueAndNow { setColor(it) } }

    override fun draw(matrixStack: UMatrixStack) {
        if (clockwise) GL11.glFrontFace(GL11.GL_CW)
        super.draw(matrixStack)
        if (clockwise) GL11.glFrontFace(GL11.GL_CCW)
    }
}
