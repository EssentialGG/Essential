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
package gg.essential.gui.multiplayer

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.data.OnboardingData
import gg.essential.elementa.components.UIImage
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.modals.TOSModal
import gg.essential.mixins.ext.client.gui.acc
import gg.essential.mixins.ext.client.gui.close
import gg.essential.mixins.ext.client.gui.essential
import gg.essential.mixins.ext.client.gui.ext
import gg.essential.mixins.ext.client.gui.refresh
import gg.essential.universal.UMatrixStack
import gg.essential.util.GuiUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.ServerSelectionList
import java.util.*

import net.minecraft.client.gui.ServerListEntryNormal
import net.minecraft.client.multiplayer.ServerData

//#if MC >= 11600
//$$ import gg.essential.util.textLiteral
//#endif

class EssentialMultiplayerGui {
    private var initialized = false

    private lateinit var screen: GuiMultiplayer
    private val width: Int get() = screen.width
    private val height: Int get() = screen.height
    private val shouldShowServerPrivacyModal: Boolean get() = !OnboardingData.hasSeenFriendsOption() && OnboardingData.hasAcceptedTos()
    private lateinit var favouritesTabButton: GuiButton
    private lateinit var friendsTabButton: GuiButton
    private lateinit var discoverTabButton: GuiButton
    private lateinit var addToFavouritesButton: GuiButton
    private var refreshButton: GuiButton? = null
    private var closeButton: GuiButton? = null
    private lateinit var featured: UIImage

    private lateinit var essentialServerList: EssentialServerSelectionList

    private val connectionManager = Essential.getInstance().connectionManager

    fun initGui(screen: GuiMultiplayer) {
        if (!EssentialConfig.essentialFull) return
        this.screen = screen

        favouritesTabButton = makeButton(100, width / 2 - 154, 36, 100, 20, "Favorites")
        friendsTabButton = makeButton(200, width / 2 - 50, 36, 100, 20, "Friends")
        discoverTabButton = makeButton(300, width / 2 + 54, 36, 100, 20, "Featured")
        addToFavouritesButton = makeButton(400, width / 2 - 75, height - 42, 75, 20, "Favorite")
        featured = EssentialPalette.FEATURED_16X.create()

        if (!initialized) {
            initialized = true

            essentialServerList = EssentialServerSelectionList(screen, screen.acc.serverListSelector)

            when (EssentialConfig.currentMultiplayerTab) {
                // 0 is already set up by vanilla
                1 -> essentialServerList.updateFriendsServers()
                2 -> essentialServerList.loadFeaturedServers()
            }

            // If the screen is not being refreshed, it is being opened for the "first time" (i.e. it was
            // on another screen before)
            if (!isRefreshing) {
                sendCurrentTabTelemetry(EssentialConfig.currentMultiplayerTab, true)
            }

            if (shouldShowServerPrivacyModal) {
                GuiUtil.pushModal { manager -> 
                    ConfirmDenyModal(manager, false).configure {
                        titleText = "Do you want your friends to see what servers you are playing on?"
                        primaryButtonText = "Yes"
                        cancelButtonText = "No"
                    }.onPrimaryAction {
                        OnboardingData.setSeenFriendsOption()
                        EssentialConfig.sendServerUpdates = true
                    }.onCancel {
                        if (it) {
                            OnboardingData.setSeenFriendsOption()
                            EssentialConfig.sendServerUpdates = false
                        }
                    }
                }
            }

            updateFriendsButton()


            // This is set by MixinGuiMultiplayer#essential$markRefresh, which occurs when the screen is being
            // refreshed. A refresh just re-displays a new instance of the screen, so if we initialized because
            // of that, we have to mark it as false again.
            isRefreshing = false
        }
    }

    fun setupButtons(
        buttons: List<GuiButton>,
        addButton: (GuiButton) -> GuiButton,
        removeButton: (GuiButton) -> GuiButton
    ) {
        if (!EssentialConfig.essentialFull) return
        val btnSelectServer = screen.acc.btnSelectServer

        fun removeAllButtons() {
            for (button in buttons) {
                removeButton(button)
            }
        }

        fun repositionJoinServerButton(makeSmall: Boolean, newText: String) {
            btnSelectServer.width = if (makeSmall) 75 else 100
            //#if MC==10809
            //$$ btnSelectServer.xPosition = width / 2 - 154
            //$$ btnSelectServer.yPosition = height - 42
            //#else
            btnSelectServer.x = width / 2 - 154
            btnSelectServer.y = height - 42
            //#endif
            //#if MC < 11400
            btnSelectServer.displayString = newText
            //#else
            //$$ btnSelectServer.message = textLiteral(newText)
            //#endif
        }

        when (EssentialConfig.currentMultiplayerTab) {
            1 -> {
                removeAllButtons()
                repositionJoinServerButton(false, "Join Friend")
                addButton(btnSelectServer)
                refreshButton = addButton(makeButton(8, (width shr 1) - 50, height - 42, 100, 20, "Refresh"))
                closeButton = addButton(makeButton(0, (width shr 1) + 54, height - 42, 100, 20, "Cancel"))
            }
            2 -> {
                removeAllButtons()
                repositionJoinServerButton(true, "Join Server")
                addButton(addToFavouritesButton)
                addButton(btnSelectServer)
                refreshButton = addButton(makeButton(8, (width shr 1) + 5, height - 42, 75, 20, "Refresh"))
                closeButton = addButton(makeButton(0, (width shr 1) + 84, height - 42, 75, 20, "Cancel"))
            }
        }

        addButton(favouritesTabButton)
        addButton(friendsTabButton)
        addButton(discoverTabButton)

        updateButtonState()
    }

    fun updateButtonState() {
        if (!initialized) return
        favouritesTabButton.enabled = EssentialConfig.currentMultiplayerTab != 0
        friendsTabButton.enabled = EssentialConfig.currentMultiplayerTab != 1
        discoverTabButton.enabled = EssentialConfig.currentMultiplayerTab != 2
        addToFavouritesButton.enabled =
            screen.acc.btnSelectServer.enabled
                && EssentialConfig.currentMultiplayerTab == 2
                && screen.acc.serverListSelector.selectedEntry?.let { !essentialServerList.isFavorite(it) } ?: false
    }

    fun onButtonClicked(button: GuiButton) {
        if (!EssentialConfig.essentialFull) return
        if (button.enabled) {
            when (button) {
                closeButton -> screen.ext.close()
                refreshButton -> screen.ext.refresh()
                favouritesTabButton -> switchTab(0)
                friendsTabButton -> withTosAccepted { switchTab(1) }
                discoverTabButton -> withTosAccepted { switchTab(2) }
                addToFavouritesButton -> {
                    val server = screen.acc.serverListSelector.selectedEntry ?: return
                    // Add the server to the favorites (vanilla server list)
                    essentialServerList.addFavorite(server)
                    updateButtonState()

                    // Switch to Favorites tab, select server and scroll to it
                    EssentialConfig.currentMultiplayerTab = 0
                    screen.ext.refresh()
                    val newScreen = Minecraft.getMinecraft().currentScreen as GuiMultiplayer
                    val newList = newScreen.acc.serverListSelector
                    //#if MC>=11600
                    //$$ val lastServer = newList.eventListeners.lastOrNull { it is NormalEntry }
                    //$$ newScreen.func_214287_a(lastServer)
                    //$$ newList.scrollAmount = Double.MAX_VALUE
                    //#else
                    val index = newScreen.serverList.countServers() - 1
                    newScreen.selectServer(index)
                    newList.scrollBy(newList.slotHeight * index)
                    //#endif
                }
            }
        }
    }

    fun onConnectToServer(serverData: ServerData) {
        // We only want to send featured server join telemetry when they are joining a server from the featured tab
        if (EssentialConfig.currentMultiplayerTab != 2) {
            return
        }

        val discovery = connectionManager.serverDiscoveryManager.findServerByAddress(serverData.serverIP) ?: return
        connectionManager.telemetryManager.enqueue(
            ClientTelemetryPacket("FEATURED_SERVER_JOIN", mapOf("server" to discovery.id))
        )
    }

    private fun withTosAccepted(block: () -> Unit) {
        if (!OnboardingData.hasAcceptedTos()) {
            GuiUtil.pushModal { TOSModal(it, unprompted = false, requiresAuth = true, { block() }) }
        } else {
            block()
        }
    }

    fun draw(matrixStack: UMatrixStack) {
        if (!EssentialConfig.essentialFull) return

        updateFriendsButton()
    }

    fun updateSpsSessions() {
        if (!EssentialConfig.essentialFull) return
        if (EssentialConfig.currentMultiplayerTab == 1) {
            essentialServerList.updateFriendsServers()
        }
    }

    fun updatePlayerActivity(uuid: UUID) {
        if (!EssentialConfig.essentialFull) return
        if (EssentialConfig.currentMultiplayerTab == 1) {
            essentialServerList.updateFriendsServers()
        }

        essentialServerList.updatePlayerStatus(uuid)
    }

    private fun switchTab(tab: Int) {
        EssentialConfig.currentMultiplayerTab = tab
        screen.ext.refresh()

        sendCurrentTabTelemetry(tab, false)
    }

    private fun sendCurrentTabTelemetry(tab: Int, initial: Boolean) {
        val tabName = when (tab) {
            0 -> "FAVORITE"
            1 -> "FRIENDS"
            2 -> "FEATURED"
            else -> "UNKNOWN"
        }

        Essential.getInstance().connectionManager.telemetryManager.enqueue(
            ClientTelemetryPacket(
                "MULTIPLAYER_SERVER_LIST_VIEW",
                mapOf("tab" to tabName, "initial" to initial)
            )
        )
    }

    private fun makeButton(id: Int, x: Int, y: Int, width: Int, height: Int, text: String): GuiButton {
        //#if MC <11400
        return GuiButton(id, x, y, width, height, text)
        //#elseif MC>=11903
        //$$ return ButtonWidget.builder(textLiteral(text)) {
        //$$     onButtonClicked(it)
        //$$ }.dimensions(x, y, width, height).build()
        //#else
        //$$ return Button(x, y, width, height, textLiteral(text)) { onButtonClicked(it) }
        //#endif
    }

    private fun updateFriendsButton() {
        val friendText = "Friends [%d]".format(essentialServerList.getFriendsServers().size)
        //#if MC<=11202
        friendsTabButton.displayString = friendText
        //#else
        //$$ friendsTabButton.message = textLiteral(friendText)
        //#endif
    }

    private val ServerSelectionList.selectedEntry
        //#if MC>=11600
        //$$ get() = (selected as? NormalEntry)?.serverData
        //#else
        get() = if (selected < 0) null else (getListEntry(selected) as? ServerListEntryNormal)?.serverData
        //#endif

    companion object {
        // We need to keep track of if the screen is refreshing for telemetry purposes.
        // `GuiMultiplayer#refresh` will just create a new instance of the screen, so we need to be able to know
        // if that instance is because of a refresh, or because the screen is actually being shown for the first time.
        var isRefreshing = false

        @JvmStatic
        fun getInstance(): EssentialMultiplayerGui? {
            return (GuiUtil.openedScreen() as? GuiMultiplayer)?.ext?.essential
        }
    }
}