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

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticAnimationTriggerPacket
import gg.essential.cosmetics.events.AnimationEventType
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.sendEmotesDisabledNotification
import gg.essential.gui.elementa.PredicatedHitTestContainer
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.FloatPosition
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.outline
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.whenHovered
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.util.onAnimationFrame
import gg.essential.gui.wardrobe.Wardrobe
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.mod.cosmetics.CosmeticOutfit
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.model.BedrockModel
import gg.essential.model.util.PlayerPoseManager
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.connectionmanager.telemetry.TelemetryManager
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.network.cosmetics.toInfra
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import gg.essential.util.GuiUtil
import gg.essential.util.MinecraftUtils
import gg.essential.util.Multithreading
import gg.essential.util.UUIDUtil
import gg.essential.util.getPerspective
import gg.essential.util.setPerspective
import gg.essential.util.textLiteral
import gg.essential.util.toState
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.client.entity.AbstractClientPlayer
import java.util.concurrent.TimeUnit


class EmoteWheel : WindowScreen(
    version = ElementaVersion.V2,
    drawDefaultBackground = false,
    restoreCurrentGuiOnClose = false,
) {
    private val essential = Essential.getInstance()
    private val cosmeticManager = essential.connectionManager.cosmeticsManager
    private val keybind = essential.keybindingRegistry.openEmoteWheel
    private val debug = BasicState(false)

    private val selectedEmoteWheel = mutableStateOf(cosmeticManager.emoteWheels.indexOfFirst { it.isSelected })

    private val emoteModelsForCurrentWheel: ListState<State<BedrockModel?>?> = memo {
        val emoteIds = cosmeticManager.emoteWheels.elementAtOrNull(selectedEmoteWheel())?.slots
            ?: return@memo emptyList()

        emoteIds.map { emoteId ->
            emoteId ?: return@map null

            val cosmetic = cosmeticManager.cosmetics().firstOrNull { it.id == emoteId }
                ?: return@map stateOf(null)

            cosmeticManager.modelLoader.getModel(
                cosmetic,
                cosmetic.defaultVariantName,
                AssetLoader.Priority.Blocking
            ).toState()
        }
    }.toListState()

    init {
        // Allow the player to move while the emote wheel is open
        //#if MC>=12000
        //$$ // Now handled by Mixin_AllowMovementDuringEmoteWheel
        //#elseif MC>=11602
        //$$ this.passEvents = true
        //#else
        this.allowUserInput = true
        //#endif

        window.layout {
            PredicatedHitTestContainer()(Modifier.fillParent()) {
                bind(emoteModelsForCurrentWheel) { emoteModels ->
                    // Odd numbers are corners; we do them first so the sides take priority on hover
                    // 5 is the middle, skip it since there's no emote there
                    for (i in (1..9 step 2).filter { it != 5 }) {
                        val index = i - if (i < 5) 1 else 2
                        EmoteWheelEntry(emoteModels.getOrNull(index), i, window, debug)()
                    }
                    for (i in 2..8 step 2) {
                        val index = i - if (i < 5) 1 else 2
                        EmoteWheelEntry(emoteModels.getOrNull(index), i, window, debug)()
                    }
                }
                row(Modifier.fillParent(), Arrangement.spacedBy(10f, FloatPosition.CENTER)) {
                    shiftButton(true)
                    column(Modifier.fillHeight(), Arrangement.spacedBy(10f, FloatPosition.CENTER)) {
                        box(Modifier.height(25f)) {
                            column(Modifier.alignVertical(Alignment.End).childBasedWidth(5f).color(EssentialPalette.BLACK.withAlpha(0.7f))) {
                                spacer(height = 4f)
                                text(selectedEmoteWheel.map { "Wheel #${it + 1}" }, Modifier.color(EssentialPalette.WHITE).shadow(EssentialPalette.TEXT_SHADOW))
                                spacer(height = 3f)
                            }
                        }
                        dummyWheel()
                        box(Modifier.height(25f)) {
                            MenuButton("Edit") {
                                Essential.getInstance().connectionManager.telemetryManager.clientActionPerformed(TelemetryManager.Actions.EMOTE_WHEEL_EDIT)
                                GuiUtil.openScreen { Wardrobe(WardrobeCategory.Emotes, initialEmoteWheel = true) }
                            }(Modifier.alignVertical(Alignment.Start))
                        }
                    }
                    shiftButton(false)
                }
            }.apply { shouldIgnore = { it.isPassThrough() } }
        }

        var equipping = false
        // Check for keybind release to equip the emote and close the wheel
        window.onAnimationFrame {
            if (!equipping && !keybind.keyBinding.isKeyDown) {
                window.focusedComponent?.let { focused ->
                    if (focused is EmoteWheelEntry && canEmote(UPlayer.getPlayer()!!)) {
                        focused.emoteModel?.getUntracked()?.let { equipEmote(it) }
                    }
                }
                displayScreen(null)
                equipping = true
            }
        }

        window.onKeyType { _, keyCode ->
            if (MinecraftUtils.isDevelopment() || System.getProperty("elementa.debug", "false") == "true") {
                if (keyCode == UKeyboard.KEY_F3) {
                    debug.set { !it }
                }
            }
        }

        window.onMouseScroll { scrollEvent ->
            // Scrolling down goes up through the emote wheel list.
            val shiftValue = when {
                scrollEvent.delta >= 1.0 -> -1
                scrollEvent.delta <= -1.0 -> 1
                else -> return@onMouseScroll
            }
            selectedEmoteWheel.set(cosmeticManager.shiftSelectedEmoteWheel(shiftValue))
        }
    }

    // Adapted from OverlayManagerImpl#isPassThrough
    private fun UIComponent.isPassThrough(): Boolean {
        return when (this) {
            is Window -> true
            is EmoteWheelEntry -> false
            is UIContainer -> parent.isPassThrough()
            is UIBlock -> getColor().alpha == 0 && parent.isPassThrough()
            else -> false
        }
    }

    override fun onScreenClose() {
        cosmeticManager.flushSelectedEmoteWheel(false)
        super.onScreenClose()
    }

    private fun LayoutScope.dummyBox() {
        box(Modifier.width(57f).heightAspect(1f))
    }

    private fun LayoutScope.dummyRow() {
        row(Arrangement.spacedBy(6f)) {
            repeat(3) { dummyBox() }
        }
    }

    private fun LayoutScope.dummyWheel() {
        column(Arrangement.spacedBy(6f)) {
            repeat(3) { dummyRow() }
        }
    }

    private fun LayoutScope.shiftButton(left: Boolean) {
        val outline = Modifier.outline(EssentialPalette.COMPONENT_SELECTED_OUTLINE, 1f)
        val arrow = if (left) EssentialPalette.ARROW_LEFT_4X7 else EssentialPalette.ARROW_RIGHT_4X7
        box(Modifier.width(15f).heightAspect(1f).color(EssentialPalette.BLACK.withAlpha(0.7f)).hoverScope().whenHovered(outline)) {
            icon(arrow, (Modifier.alignHorizontal(Alignment.Center(!left)).alignVertical(Alignment.Center)).color(EssentialPalette.TEXT_HIGHLIGHT))
        }.onLeftClick { selectedEmoteWheel.set(cosmeticManager.shiftSelectedEmoteWheel(if (left) -1 else 1)) }
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    companion object {
        private const val emoteTransitionTimeMs = 0L

        const val SLOTS = 8

        @JvmField
        var isPlayerArmRendering = false
        @JvmField
        var emoteClicked = false

        @JvmField
        var emoteComing = false

        private var latestInvocation = 0
        private var savedThirdPerson = getPerspective()

        private val referenceHolder = ReferenceHolderImpl()

        init {
            EssentialConfig.disableEmotesState.onSetValue(referenceHolder) {
                unequipCurrentEmote()
            }
        }

        @JvmStatic
        fun open() {
            if (UMinecraft.getMinecraft().currentScreen != null || emoteClicked) {
                return
            }

            // We allow the user to play emotes if they were connected to the CM before (they have emote wheels loaded).
            val connectionManager = Essential.getInstance().connectionManager
            if (connectionManager.cosmeticsManager.emoteWheels.isEmpty() && !connectionManager.isAuthenticated) {
                Notifications.error(
                    "Essential Network Error",
                    "Unable to establish connection with the Essential Network."
                ) {
                    uniqueId = object {}.javaClass
                }
                return
            }

            if (EssentialConfig.disableEmotes) {
                sendEmotesDisabledNotification()
                return
            }

            connectionManager.telemetryManager.clientActionPerformed(TelemetryManager.Actions.EMOTE_WHEEL_ACTIVATE)
            GuiUtil.openScreen { EmoteWheel() }
        }

        @JvmStatic
        fun canEmote(player: AbstractClientPlayer): Boolean {
            return (player.isEntityAlive && !player.isSpectator && !player.isSneaking && !player.isPlayerSleeping && !player.isRiding
                    //#if MC>=11602
                    //$$ && !player.isSwimming()
                    //#endif
                    //#if MC>=11202
                    && !player.isElytraFlying
                    //#endif
                    )
        }

        fun getEmoteTransitionTime(cosmetic: Cosmetic): Long {
            return (cosmetic.property<CosmeticProperty.TransitionDelay>()?.data?.time)
                ?: emoteTransitionTimeMs
        }

        fun equipEmote(emote: BedrockModel) {
            val essential = Essential.getInstance()
            val connectionManager = essential.connectionManager
            val cosmeticManager = connectionManager.cosmeticsManager

            connectionManager.telemetryManager.clientActionPerformed(TelemetryManager.Actions.EMOTE_ACTIVATE, emote.cosmetic.id)

            emoteComing = true

            val animLength = emote.animationEvents
                .filter { it.type == AnimationEventType.EMOTE }
                .maxOfOrNull { it.getTotalTime(emote) }
                ?: 0f

            val slot = CosmeticSlot.EMOTE
            // If there's already an emote equipped, then we don't need to change the perspective or register a new listener
            if (cosmeticManager.equippedCosmetics[slot] == null) {
                if (EssentialConfig.thirdPersonEmotes) {
                    // Save current perspective and handle third-person view
                    savedThirdPerson = getPerspective()

                    // Only change perspective if player is in first-person
                    if (savedThirdPerson == 0) {
                        setPerspective(EssentialConfig.emoteThirdPersonType + 1)
                    }
                }
                Essential.EVENT_BUS.register(EmoteEventListeners())
            }

            val startDelay = if (cosmeticManager.equippedCosmetics[slot] == null) {
                getEmoteTransitionTime(emote.cosmetic)
            } else {
                0f
            }

            Multithreading.scheduleOnMainThread({
                emoteComing = false

                if (cosmeticManager.equippedCosmetics[slot] == emote.cosmetic.id) {
                    essential.animationEffectHandler.triggerEvent(UUIDUtil.getClientUUID(), slot, "reset")
                    connectionManager.send(ClientCosmeticAnimationTriggerPacket(slot.toInfra(), "reset"))
                } else {
                    cosmeticManager.updateEquippedCosmetic(slot, emote.cosmetic.id)
                }

                val invocationId = ++latestInvocation
                if (animLength != Float.POSITIVE_INFINITY) { // Non-looping emote
                    // Once emote has finished its animation, unequip it
                    Multithreading.scheduleOnMainThread({
                        if (invocationId == latestInvocation) {
                            unequipCurrentEmote()
                        }
                    }, ((animLength - PlayerPoseManager.transitionTime) * 1000).toLong(), TimeUnit.MILLISECONDS)
                } else if (!emote.cosmetic.emoteInterruptionTriggers.movement) { // Movement doesn't cancel this emote and it loops

                    UKeyboard.getKeyName(UMinecraft.getSettings().keyBindSneak)?.let { keybind ->
                        //#if MC>=11202
                        UPlayer.getPlayer()?.sendStatusMessage(textLiteral("Press $keybind to Stop Emote"), true)
                        //#else
                        //$$ UMinecraft.getMinecraft().ingameGUI.setRecordPlaying(textLiteral("Press $keybind to Stop Emote"), false)
                        //#endif
                    }
                }
            }, startDelay.toLong(), TimeUnit.MILLISECONDS)
        }

        @JvmOverloads
        @JvmStatic
        fun unequipCurrentEmote(outfit: CosmeticOutfit? = null) {
            val cosmeticsManager = Essential.getInstance().connectionManager.cosmeticsManager

            if (cosmeticsManager.equippedCosmetics[CosmeticSlot.EMOTE] == null) return

            val cosmetic = cosmeticsManager.getCosmetic(cosmeticsManager.equippedCosmetics[CosmeticSlot.EMOTE] ?: return) ?: return

            if (outfit != null) {
                cosmeticsManager.updateEquippedCosmetic(outfit, CosmeticSlot.EMOTE, null)
            } else {
                cosmeticsManager.updateEquippedCosmetic(CosmeticSlot.EMOTE, null)
            }

            Multithreading.scheduleOnMainThread({
                if (EssentialConfig.thirdPersonEmotes) {
                    // Save current perspective to use for next emote
                    EssentialConfig.emoteThirdPersonType = getPerspective() - 1

                    setPerspective(savedThirdPerson)
                }
                latestInvocation++
            }, getEmoteTransitionTime(cosmetic), TimeUnit.MILLISECONDS)

        }
    }
}
