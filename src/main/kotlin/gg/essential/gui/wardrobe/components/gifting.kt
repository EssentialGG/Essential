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
@file:JvmName("Gifting")
package gg.essential.gui.wardrobe.components

import gg.essential.Essential
import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticBulkRequestUnlockStatePacket
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticBulkRequestUnlockStateResponsePacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.EssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.addAll
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.isNotEmpty
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.remove
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.modals.select.users
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.content.CosmeticPreviewToastComponent
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.Wardrobe
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.giftCosmeticOrEmote
import gg.essential.gui.wardrobe.modals.CoinsPurchaseModal
import gg.essential.handlers.EssentialSoundManager
import gg.essential.network.connectionmanager.coins.CoinsManager
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.ChatColor
import gg.essential.universal.UMinecraft
import gg.essential.universal.USound
import gg.essential.util.CachedAvatarImage
import gg.essential.util.GuiUtil
import gg.essential.util.UUIDUtil
import gg.essential.util.executor
import gg.essential.vigilance.utils.onLeftClick
import java.util.UUID

fun openGiftModal(item: Item.CosmeticOrEmote, state: WardrobeState) {
    val requiredCoinsSpent = state.cosmeticsManager.wardrobeSettings.giftingCoinSpendRequirement.get()
    val coinsSpent = state.coinsSpent.get()
    if (coinsSpent < requiredCoinsSpent) {
        GuiUtil.pushModal { manager -> 
            val modal = EssentialModal(manager, false).configure {
                titleText = "You can't gift yet..."
                titleTextColor = EssentialPalette.MODAL_WARNING
                primaryButtonText = "Okay!"
            }

            val textModifier = Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK)

            modal.configureLayout { customContent ->
                customContent.layoutAsColumn {
                    spacer(height = 13f)
                    text("You can only send gifts", textModifier)
                    spacer(height = 4f)
                    row {
                        text("after spending $requiredCoinsSpent ", textModifier)
                        spacer(width = 1f)
                        image(EssentialPalette.COIN_7X)
                    }
                    spacer(height = 17f)
                }
            }
        }
        return
    }

    // Get all friends except those who already own the item to gift
    val connectionManager = Essential.getInstance().connectionManager
    val allFriends = connectionManager.relationshipManager.friends.keys.toList()
    val validFriends = mutableListStateOf<UUID>()
    val loadingFriends = mutableStateOf(allFriends.isNotEmpty())

    connectionManager.send(ClientCosmeticBulkRequestUnlockStatePacket(allFriends.toSet(), item.cosmetic.id)) { maybePacket ->
        ServerCosmeticBulkRequestUnlockStateResponsePacket::class.java // FIXME workaround for feature-flag-processor eating the packet
        when (val packet = maybePacket.orElse(null)) {
            is ServerCosmeticBulkRequestUnlockStateResponsePacket -> validFriends.addAll(packet.unlockStates.filter { !it.value }.keys.toList())
            else -> {
                showErrorToast("Something went wrong, please try again.")
                val prefix = (packet as? ResponseActionPacket)?.let { "$it - " } ?: ""
                Essential.logger.error(prefix + "Failed to validate unlock status for ${item.cosmetic.displayName} in friends list.")
            }
        }
        loadingFriends.set(false)
    }

    fun LayoutScope.addRemoveCheckbox(selected: MutableState<Boolean>) {
        val hoverColor = selected.map { if (it) EssentialPalette.CHECKBOX_SELECTED_BACKGROUND_HOVER else EssentialPalette.CHECKBOX_BACKGROUND_HOVER }
        val colorModifier = Modifier.color(EssentialPalette.CHECKBOX_BACKGROUND)
            .whenTrue(!selected, Modifier.outline(EssentialPalette.CHECKBOX_OUTLINE, 1f, true))
            .whenTrue(selected, Modifier.color(EssentialPalette.CHECKBOX_SELECTED_BACKGROUND))

        box(colorModifier.width(9f).heightAspect(1f).hoverScope().hoverColor(hoverColor)) {
            if_(selected) {
                image(EssentialPalette.CHECKMARK_7X5, Modifier.color(EssentialPalette.TEXT_HIGHLIGHT))
            }
        }
    }

    val selectedFriends = mutableListStateOf<UUID>()
    GuiUtil.pushModal { manager -> 
        selectModal(manager, "Select friends to\ngift them ${ChatColor.WHITE + item.name + ChatColor.RESET}.") {
            modalSettings {
                primaryButtonText = "Purchase"
                titleTextColor = EssentialPalette.TEXT
            }

            emptyText(loadingFriends.map { loading ->
                when {
                    allFriends.isEmpty() -> "You haven't added any friends yet. You can add them in the social menu."
                    loading -> "Loading..."
                    else -> "Your friends already own this item."
                }
            })

            requiresButtonPress = false
            requiresSelection = true

            users("Friends", validFriends) {selected, uuid ->
                row(Modifier.fillParent(padding = 3f)) {
                    playerEntry(selected, uuid)
                    addRemoveCheckbox(selected)
                }.onLeftClick { event ->
                    USound.playButtonPress()
                    event.stopPropagation()
                    selected.set { !it }
                }
            }

            extraContent = {
                val quantityText = selectedFriends.map { "${it.size}x ${item.name}" }
                val costText = memo { CoinsManager.COIN_FORMAT.format((item.getCost(state)() ?: 0) * selectedFriends().size) }
                val shadowModifier = Modifier.shadow(EssentialPalette.BLACK)

                if_(validFriends.isNotEmpty()) {
                    column(Modifier.fillWidth(rightPadding = 1f)) {
                        spacer(height = 10f)
                        row(Modifier.fillWidth(), Arrangement.SpaceBetween) {
                            text(quantityText, shadowModifier.color(EssentialPalette.TEXT_MID_GRAY))
                            row(Arrangement.spacedBy(2f)) {
                                text(costText, shadowModifier.color(EssentialPalette.TEXT))
                                image(EssentialPalette.COIN_7X, shadowModifier)
                            }
                        }
                        spacer(height = 1f)
                    }
                }
            }
        }.onSelection {uuid, selected ->
            if (selected) {
                selectedFriends.add(uuid)
            } else {
                selectedFriends.remove(uuid)
            }
        }.apply {
            onPrimaryAction { selectedUsers ->
                giftItemToFriends(item, selectedUsers, state, this)
            }
        }
    }
}

fun openWardrobeWithHighlight(item: Item) {
    val openedScreen = GuiUtil.openedScreen()
    if (openedScreen is Wardrobe) {
        openedScreen.state.highlightItem.set(item.itemId)
    } else {
        GuiUtil.openScreen {
            // Change initial category to stop the highlighted item highlighting on the featured page
            Wardrobe(WardrobeCategory.Cosmetics).apply { state.highlightItem.set(item.itemId) }
        }
    }
}

private fun showErrorToast(message: String) {
    Notifications.push("Gifting failed", message) { type = NotificationType.ERROR }
}

private fun giftItemToFriends(item: Item.CosmeticOrEmote, uuids: Set<UUID>, state: WardrobeState, modal: EssentialModal) {
    val cost = (item.getCost(state).get() ?: 0) * uuids.size
    if (cost > state.coins.get()) {
        modal.replaceWith(CoinsPurchaseModal(modal.modalManager, state, cost))
        return
    }

    val connectionManager = Essential.getInstance().connectionManager

    for (uuid in uuids) {
        UUIDUtil.getName(uuid).whenCompleteAsync({ username, exception ->
            if (exception != null) {
                showErrorToast("Something went wrong, please try again.")
                Essential.logger.error("Failed to lookup username for $uuid", exception)
                return@whenCompleteAsync
            }

            state.giftCosmeticOrEmote(item, uuid) { success, errorCode ->
                if (!success) {
                    val errorMessage = when (errorCode) {
                        "TARGET_MUST_BE_FRIEND" -> "$username is not your friend!"
                        "ESSENTIAL_USER_NOT_FOUND" -> "$username is not an Essential user!"
                        "Cosmetic already unlocked." -> "$username already owns this item!"
                        else -> "Something went wrong gifting to $username, please try again."
                    }
                    showErrorToast(errorMessage)
                    return@giftCosmeticOrEmote
                }
                modal.replaceWith(null)
                EssentialSoundManager.playPurchaseConfirmationSound()
                showGiftSentToast(item.cosmetic, username)
                connectionManager.chatManager.sendGiftEmbed(uuid, item.cosmetic.id)
            }
        }, UMinecraft.getMinecraft().executor)
    }
}

fun showGiftSentToast(cosmetic: Cosmetic, username: String) {
    Notifications.push("", "${ChatColor.WHITE + cosmetic.displayName + ChatColor.RESET} has been gifted to $username.") {
        withCustomComponent(Slot.ACTION, CosmeticPreviewToastComponent(cosmetic))
    }
}

fun showGiftReceivedToast(cosmeticId: String, uuid: UUID, username: String) {
    val cosmetic = Essential.getInstance().connectionManager.cosmeticsManager.getCosmetic(cosmeticId) ?: return
    Notifications.push(username, "", 4f, {
        openWardrobeWithHighlight(Item.CosmeticOrEmote(cosmetic))
    }) {
        withCustomComponent(Slot.ICON, CachedAvatarImage.create(uuid))
        withCustomComponent(Slot.SMALL_PREVIEW, CosmeticPreviewToastComponent(cosmetic))
        withCustomComponent(Slot.LARGE_PREVIEW, UIContainer().apply {
            val colorModifier = Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.TEXT_SHADOW_LIGHT)
            layout(Modifier.fillWidth().childBasedHeight()) {
                wrappedText("{gift} Sent you a gift", Modifier.alignHorizontal(Alignment.Start), colorModifier) {
                    "gift" { icon(EssentialPalette.WARDROBE_GIFT_7X, colorModifier) }
                }
            }
        })
    }
}
