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
package gg.essential.gui.wardrobe

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.MousePositionConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.set
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.onAnimationFrame
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.UMouse
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.abs

class EmoteWheelPage(private val state: WardrobeState) : UIContainer() {
    init {
        val containerModifier = Modifier.childBasedMaxSize()
        layout {
            box(containerModifier) {
                column(Arrangement.spacedBy(7f)) {
                    row(Arrangement.spacedBy(7f)) {
                        emoteSlot(slotModifier, 0)
                        emoteSlot(slotModifier, 1)
                        emoteSlot(slotModifier, 2)
                    }
                    row(Arrangement.spacedBy(7f)) {
                        emoteSlot(slotModifier, 3)
                        box(slotModifier)
                        emoteSlot(slotModifier, 4)
                    }
                    row(Arrangement.spacedBy(7f)) {
                        emoteSlot(slotModifier, 5)
                        emoteSlot(slotModifier, 6)
                        emoteSlot(slotModifier, 7)
                    }
                }
            }
        }
    }

    private fun LayoutScope.emoteSlot(modifier: Modifier, index: Int) {
        val cartHovered = BasicState(false).map { it }
        val emote = state.emoteWheel.map { it[index] }
        val cosmetic = emote.map { it?.let { id -> state.cosmeticsManager.getCosmetic(id) } }
        val empty = emote.map { it == null }
        val filledButNotOwned = !empty and cosmetic.zip(state.unlockedCosmetics).map { (cosmetic, unlockCosmetics) -> cosmetic?.let { it.id !in unlockCosmetics } ?: false }
        val hovered = mutableStateOf(false)
        val draggingInProgress = state.draggingEmoteSlot.map { it != null }
        val beingDraggedFrom = state.draggingEmoteSlot.map { it == index } and !empty
        val beingDraggedOnto = state.draggingOntoEmoteSlot.map { it == index } and (state.draggingEmoteSlot.zip(state.emoteWheel).map { (index, emoteWheel ) -> index != null && (index == -1 || emoteWheel[index] != null) })
        val visibleCosmetic = cosmetic.zip(beingDraggedFrom).map { (cosmetic, beingDraggedFrom) -> cosmetic.takeUnless { beingDraggedFrom } }

        val backgroundColor = stateBy {
            when {
                beingDraggedOnto() -> EssentialPalette.COMPONENT_HIGHLIGHT
                empty() -> EssentialPalette.INPUT_BACKGROUND
                hovered() -> EssentialPalette.COMPONENT_HIGHLIGHT
                else -> EssentialPalette.COMPONENT_BACKGROUND
            }
        }

        val outline = Modifier.outline(
            color = BasicState(EssentialPalette.TEXT),
            width = beingDraggedOnto.map { if (it) 1f else 0f }.toV1(this@EmoteWheelPage),
        )

        val fadeOut = Modifier.effect { FadeEffect(backgroundColor.toV1(this@EmoteWheelPage), 0.3f) }

        fun LayoutScope.thumbnail(cosmetic: Cosmetic, modifier: Modifier): UIComponent {
            return CosmeticPreview(cosmetic)(modifier)
        }

        val container = EmoteSlot(index)
        container(modifier.color(backgroundColor).then(outline)) {
            ifNotNull(visibleCosmetic) { cosmetic ->
                box(Modifier.fillParent().whenTrue(beingDraggedOnto, fadeOut)) {
                    thumbnail(cosmetic, Modifier.fillParent())
                }
            }
        }

        container.hoveredState().onSetValueAndNow { hovered.set(it) }

        val draggable =
            object : UIContainer() {
                // when we hitTest, we want the thing below the dragging graphic
                override fun isPointInside(x: Float, y: Float): Boolean = false
            }
        draggable.layout(Modifier.width(container).height(container)) {
            ifNotNull(cosmetic) { cosmetic ->
                thumbnail(cosmetic, Modifier.fillParent())
            }
        }

        fun tooltip(parent: UIComponent, text: State<String>) =
            EssentialTooltip(parent, position = EssentialTooltip.Position.RIGHT, notchSize = 0)
                .constrain {
                    x = MousePositionConstraint() + 10.pixels
                    y = MousePositionConstraint() - 15.pixels
                }
                .bindLine(text.toV1(this@EmoteWheelPage))


        val nameTooltip by tooltip(container, cosmetic.map { it?.displayName ?: "" })
        val swapTooltip by tooltip(draggable, stateOf("Swap"))
        val removeTooltip by tooltip(draggable, stateOf("Remove"))

        val swapVisible = beingDraggedFrom and state.draggingOntoOccupiedEmoteSlot
        val removeVisible = beingDraggedFrom and state.draggingOntoEmoteSlot.map { it == -1 }
        swapTooltip.bindVisibility(swapVisible)
        removeTooltip.bindVisibility(removeVisible)
        nameTooltip.bindVisibility((hovered and !draggingInProgress and !empty and !cartHovered.toV2()) or (beingDraggedFrom and !swapVisible and !removeVisible))

        draggable.onAnimationFrame {
            val (mouseX, mouseY) = getMousePosition()
            val target = Window.of(draggable).hitTest(mouseX, mouseY)
            val slotTarget = target as? EmoteSlot ?: target.findParentOfTypeOrNull<EmoteSlot>()
            val removeTarget = target.findParentOfTypeOrNull<WardrobeContainer>()
            state.draggingOntoEmoteSlot.set(when {
                slotTarget != null -> slotTarget.index.takeIf { it != index }
                removeTarget != null -> -1
                else -> null
            })
        }

        var mayBeLeftClick = false
        var clickStart = Pair(0f, 0f)
        container.onLeftClick {
            val xOffset = UMouse.Scaled.x.toFloat() - container.getLeft()
            val yOffset = UMouse.Scaled.y.toFloat() - container.getTop()
            draggable.constrain {
                x = MousePositionConstraint() - xOffset.pixels
                y = MousePositionConstraint() - yOffset.pixels
            }
            Window.of(this).addChild(draggable)

            state.draggingEmoteSlot.set(index)

            mayBeLeftClick = true
            clickStart = Pair(xOffset, yOffset)
        }

        container.onMouseDrag { mouseX, mouseY, _ ->
            val distance = abs(clickStart.first - mouseX) + abs(clickStart.second - mouseY)
            if (distance > 5) {
                mayBeLeftClick = false
            }
        }

        draggable.onMouseRelease {
            val target = state.draggingOntoEmoteSlot.get()
            if (target != null) {
                if (target == -1) {
                    state.emoteWheelManager.setEmote(index, null)
                } else {
                    val sourceEmote = state.emoteWheel.getUntracked()[index]
                    val targetEmote = state.emoteWheel.getUntracked()[target]
                    state.emoteWheelManager.setEmote(index, targetEmote)
                    state.emoteWheelManager.setEmote(target, sourceEmote)
                }
            } else if (mayBeLeftClick) {
                state.emoteWheel.getUntracked()[index]?.let {
                    USound.playButtonPress()
                }
                state.emoteWheelManager.setEmote(index, null)
            }

            state.draggingEmoteSlot.set(null)
            state.draggingOntoEmoteSlot.set(null)

            Window.enqueueRenderOperation {
                hide(instantly = true)
            }
        }
    }

    class EmoteSlot(val index: Int) : UIBlock()

    companion object {
        val slotModifier = Modifier.width(62f).height(62f)
    }

}
