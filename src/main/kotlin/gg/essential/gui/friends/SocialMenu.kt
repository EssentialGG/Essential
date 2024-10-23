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
package gg.essential.gui.friends

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.Essential
import gg.essential.api.gui.GuiRequiresTOS
import gg.essential.config.EssentialConfig
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.bindConstraints
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.friends.message.v2.getInfraInstance
import gg.essential.gui.friends.previews.ChannelPreview
import gg.essential.gui.friends.state.SocialStateManager
import gg.essential.gui.friends.tabs.ChatTab
import gg.essential.gui.friends.tabs.FriendsTab
import gg.essential.gui.friends.title.SocialTitleManagementActions
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.modals.select.users
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.warning
import gg.essential.gui.util.onItemAdded
import gg.essential.gui.util.toStateV2List
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import java.util.*

class SocialMenu @JvmOverloads constructor(
    channelIdToOpen: Long? = null
): InternalEssentialGUI(
    ElementaVersion.V6,
    "Social",
    discordActivityDescription = "Messaging friends",
), GuiRequiresTOS {

    private val connectionManager = Essential.getInstance().connectionManager
    private val spsManager = connectionManager.spsManager

    val socialStateManager = SocialStateManager(connectionManager)

    var selectedTab = mutableStateOf(Tab.CHAT)

    val dividerWidth = rightDivider.getWidth()

    val tabsSelector by TabsSelector(selectedTab).constrain {
        width = 215.pixels.coerceAtMost(50.percent).coerceAtLeast(ChildBasedSizeConstraint())
        height = 27.pixels
    } childOf content

    val chatTab by ChatTab(this, selectedTab)
    val friendsTab by FriendsTab(this, selectedTab)
    private val titleManagementActions by SocialTitleManagementActions(this).constrain {
        y = CenterPixelConstraint()
    }.bindConstraints(selectedTab) {
        x = if (it == Tab.CHAT) {
            10.pixels(alignOpposite = true) boundTo tabsSelector
        } else {
            10.pixels(alignOpposite = true)
        }
    } childOf titleBar

    private val tabs = listOf(chatTab, friendsTab)

    /**
     * The channel ID of the channel that was open when messenger states reset.
     */
    private var channelToRestore: Long? = null

    init {
        titleManagementActions.search.textContent.onSetValue { username ->
            tabs.forEach {
                it.search(username)
            }
        }
        chatTab.populate()
        friendsTab.populate()

        if (channelIdToOpen != null) {
            val preview = chatTab[connectionManager.chatManager.mergeAnnouncementChannel(channelIdToOpen)]
            if (preview != null) {
                openMessageScreen(preview)
            } else {
                Essential.logger.error("Unable to find channel with ID $channelIdToOpen")
            }
        } else {
            chatTab.openTopChannel()
        }

        socialStateManager.messengerStates.registerResetListener {
            channelToRestore = chatTab.currentMessageView.get()?.preview?.channel?.id
        }

        socialStateManager.messengerStates.getObservableChannelList().onItemAdded {
            if (it.id == channelToRestore) {

                channelToRestore = null

                // In order to ensure this functions correctly, two delays is required. The order that
                // observers are called is not guaranteed the Java spec, and the call to openMessageScreen
                // must be after the chatTab has processed channels after connect.
                Window.enqueueRenderOperation {
                    Window.enqueueRenderOperation {
                        openMessageScreen(it)
                    }
                }
            }
        }
    }

    // Prevents multiple EssentialMarkdown components from having a selection at the same time
    // Linear: EM-1973
    override fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        if (mouseButton != 0 || selectedTab.get() != Tab.CHAT) {
            super.onMouseClicked(mouseX, mouseY, mouseButton)
            return
        }

        // A general floating check would be better, but Elementa has no way to do that at the moment.
        val isHittingContextMenu = window
            .hitTest(mouseX.toFloat(), mouseY.toFloat())
            .findParentOfTypeOrNull<ContextOptionMenu>() != null

        if (!isHittingContextMenu) {
            chatTab.findChildrenOfType<EssentialMarkdown>(recursive = true).forEach {
                it.clearSelection()
            }
        }

        super.onMouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateGuiScale() {
        newGuiScale = GuiUtil.getGuiScale()
        super.updateGuiScale()
    }

    fun openMessageScreen(preview: ChannelPreview) {
        chatTab.openMessage(preview)
        selectedTab.set(Tab.CHAT)
    }

    fun openMessageScreen(channel: Channel) {
        chatTab[channel.id]?.let { openMessageScreen(it) }
    }

    fun openMessageFor(user: UUID) {
        val channelPreview = chatTab[user]
        if (channelPreview != null) {
            openMessageScreen(channelPreview)
        }
    }

    fun showManagementDropdown(
        preview: ChannelPreview,
        position: ContextOptionMenu.Position,
        extraOptions: List<ContextOptionMenu.Item> = emptyList(),
        onClose: () -> Unit
    ) {
        when {
            preview.otherUser != null -> {
                showUserDropdown(preview.otherUser, position, extraOptions.toMutableList().apply {
                    addMarkMessagesReadOption(preview.channel.id, this)
                }, onClose)
            }
            !preview.channel.isAnnouncement() -> showGroupDropdown(preview.channel, position, extraOptions, onClose)
            else -> onClose.invoke()
        }
    }

    private fun addMarkMessagesReadOption(
        channelId: Long,
        options: MutableList<ContextOptionMenu.Item>,
    ) {
        if (socialStateManager.messengerStates.getUnreadChannelState(channelId).getUntracked()) {
            options.add(ContextOptionMenu.Option("Mark as Read", image = EssentialPalette.MARK_UNREAD_10X7) {
                socialStateManager.messengerStates.getMessageListState(channelId).getUntracked().forEach {
                    socialStateManager.messengerStates.setUnreadState(it.getInfraInstance(), false)
                }
            })
        }
    }

    fun showUserDropdown(user: UUID, position: ContextOptionMenu.Position, onClose: () -> Unit) {
        showUserDropdown(user, position, emptyList(), onClose)
    }

    fun showUserDropdown(
        user: UUID,
        position: ContextOptionMenu.Position,
        extraOptions: List<ContextOptionMenu.Item>,
        onClose: () -> Unit
    ) {
        val options = extraOptions.toMutableList()

        val joinPlayerOption = ContextOptionMenu.Option(
            "Join",
            // New default is text, so remove entirely when removing feature flag
            textColor = EssentialPalette.TEXT,
            hoveredColor = EssentialPalette.MESSAGE_SENT,
            // New default is black, so remove entirely when removing feature flag
            shadowColor = EssentialPalette.BLACK,
            image = EssentialPalette.JOIN_ARROW_5X,
        ) {
            handleJoinSession(user)
        }
        val invitePlayerOption = ContextOptionMenu.Option(
            "Invite",
            // New default is text, so remove entirely when removing feature flag
            textColor = EssentialPalette.TEXT,
            hoveredColor = EssentialPalette.MESSAGE_SENT,
            // New default is black, so remove entirely when removing feature flag
            shadowColor = EssentialPalette.BLACK,
            image = EssentialPalette.INVITE_10X6,
        ) {
            handleInvitePlayers(setOf(user))
        }

        val topmostOptions: MutableList<ContextOptionMenu.Item> = mutableListOf()

        if (socialStateManager.statusStates.getActivity(user).isJoinable()) {
            topmostOptions.add(joinPlayerOption)
        }
        if (ServerType.current()?.supportsInvites == true) {
            topmostOptions.add(invitePlayerOption)
        }

        if (topmostOptions.isNotEmpty()) {
            options.add(0, ContextOptionMenu.Divider)

            for (optionItem in topmostOptions) {
                options.add(0, optionItem)
            }
        }

        // Don't add a divider below is we haven't added anything above here
        var addedDivider = extraOptions.isEmpty()

        val messageScreen = chatTab.currentMessageView.get()
        if (selectedTab.get() == Tab.FRIENDS) {
            options.add(ContextOptionMenu.Option("Send Message", image = EssentialPalette.MESSAGE_10X6) {
                openMessageFor(user)
            })
            // We always add this divider as it's below the option
            options.add(ContextOptionMenu.Divider)
            addedDivider = true
        } else if (messageScreen != null) {
            val channel = socialStateManager.messengerStates.getObservableChannelList().firstOrNull {
                it.getOtherUser() == user
            }
            if (channel != null) {
                val muted = socialStateManager.messengerStates.getMuted(channel.id)

                if (!addedDivider) {
                    options.add(ContextOptionMenu.Divider)
                    addedDivider = true
                }
                options.add(ContextOptionMenu.Option(
                    {
                        if (muted()) {
                            "Unmute Friend"
                        } else {
                            "Mute Friend"
                        }
                    },
                    image = {
                        if (muted()) {
                            EssentialPalette.UNMUTE_8X9
                        } else {
                            EssentialPalette.MUTE_8X9
                        }
                    },
                ) {
                    muted.set { !it }
                })
            }

        }
        if (!addedDivider) {
            options.add(ContextOptionMenu.Divider)
        }

        val blocked = isBlocked(user)

        if (!blocked) {
            options.add(ContextOptionMenu.Option(
                if (isFriend(user)) {
                    "Remove Friend"
                } else {
                    "Add Friend"
                },
                image = if (isFriend(user)) EssentialPalette.REMOVE_FRIEND_10X5 else EssentialPalette.INVITE_10X6,
            ) {
                handleAddOrRemove(user)
            })
        }

        options.add(ContextOptionMenu.Option(
            if (blocked) {
                "Unblock"
            } else {
                "Block"
            },
            image = EssentialPalette.BLOCK_10X7,
            hoveredColor = EssentialPalette.TEXT_WARNING
        ) {
            handleBlockOrUnblock(user)
        })
        ContextOptionMenu.create(position, window, *options.toTypedArray(), onClose = onClose)
    }

    fun showGroupDropdown(
        channel: Channel,
        position: ContextOptionMenu.Position,
        extraOptions: List<ContextOptionMenu.Item>,
        onClose: () -> Unit
    ) {
        val options = extraOptions.toMutableList()

        addMarkMessagesReadOption(channel.id, options)

        if (ServerType.current()?.supportsInvites == true) {
            options.add(
                ContextOptionMenu.Option(
                    "Invite Group",
                    // New default is text, so remove entirely when removing feature flag
                    textColor = EssentialPalette.TEXT,
                    hoveredColor = EssentialPalette.MESSAGE_SENT,
                    // New default is black, so remove entirely when removing feature flag
                    shadowColor = EssentialPalette.BLACK,
                    image = EssentialPalette.INVITE_10X6,
                ) {
                    handleInvitePlayers(channel.members)
                }
            )

            options.add(ContextOptionMenu.Divider)
        }

        val mutedState = socialStateManager.messengerStates.getMuted(channel.id)
        if (channel.type == ChannelType.GROUP_DIRECT_MESSAGE && channel.createdInfo.by == UUIDUtil.getClientUUID()) {
            options.add(ContextOptionMenu.Option(
                "Invite Friends",
                image = EssentialPalette.MARK_UNREAD_10X7
            ) {
                // We don't want to show anyone currently in the group here
                val potentialFriends = socialStateManager.relationshipStates.getObservableFriendList()
                    .toStateV2List()
                    .filter {
                        !channel.members.contains(it)
                    }

                GuiUtil.pushModal { manager ->
                    selectModal<UUID>(manager, "Add Friends to Group") {
                        requiresButtonPress = false
                        requiresSelection = true

                        users("Friends", potentialFriends)
                    }.onPrimaryAction { users ->
                        socialStateManager.messengerStates.addMembers(channel.id, users)
                    }
                }
            })
            options.add(ContextOptionMenu.Divider)
            options.add(ContextOptionMenu.Option(
                "Rename Group",
                image = EssentialPalette.PENCIL_7x7
            ) {
                GuiUtil.pushModal { manager ->
                    CancelableInputModal(manager, "", channel.name, maxLength = 24).configure {
                        titleText = "Rename Group"
                        contentText = "Enter a new name for your group."
                        primaryButtonText = "Rename"
                    }.mapInputToEnabled {
                        it.isNotBlank() && it != channel.name
                    }.onPrimaryActionWithValue { it ->
                        socialStateManager.messengerStates.setTitle(channel.id, it)
                    }
                }
            })
            options.add(ContextOptionMenu.Divider)
        }

        options.add(ContextOptionMenu.Option({
            if (mutedState()) {
                "Unmute Group"
            } else {
                "Mute Group"
            }
        }, image = {
            if (mutedState()) {
                EssentialPalette.UNMUTE_8X9
            } else {
                EssentialPalette.MUTE_8X9
            }
        }) {
            mutedState.set { !it } // Will be automatically applied properly
        })

        options.add(ContextOptionMenu.Option("Leave Group", image = EssentialPalette.LEAVE_10X7) {
            GuiUtil.pushModal { manager ->
                ConfirmDenyModal(manager, false).configure {
                    titleText = "Are you sure you want to leave this group?"
                    primaryButtonText = "Confirm"
                }.onPrimaryAction {
                    socialStateManager.messengerStates.leaveGroup(channel.id)
                }
            }
        })

        ContextOptionMenu.create(position, window, *options.toTypedArray(), onClose = onClose)
    }

    fun handleJoinSession(user: UUID) {
        if (UMinecraft.getWorld() != null) {
            if (!socialStateManager.statusStates.joinSession(user)) {
                Notifications.warning("World invite expired", "")
            }
            return
        }

        UUIDUtil.getName(user).thenAcceptOnMainThread {
            val isSps = spsManager.remoteSessions.any { it.hostUUID == user }
            val title = buildString {
                append("Are you sure you want to join $it's ")
                if (isSps) {
                    append("world")
                } else {
                    append("server")
                }
                append("?")
            }
            GuiUtil.pushModal { manager ->
                ConfirmDenyModal(manager, false).configure {
                    titleText = title
                }.onPrimaryAction {
                    if (!socialStateManager.statusStates.joinSession(user)) {
                        Notifications.warning("World invite expired", "")
                    }
                }
            }
        }
    }

    fun handleInvitePlayers(users: Set<UUID>) {
        val currentServerData = UMinecraft.getMinecraft().currentServerData
        if (hasLocalSession()) {
            spsManager.reinviteUsers(users)
        } else if (currentServerData != null) {
            connectionManager.socialManager.reinviteFriendsOnServer(currentServerData.serverIP, users)
        }
    }

    private fun hasLocalSession() = spsManager.localSession != null

    private fun isBlocked(uuid: UUID) = uuid in socialStateManager.relationshipStates.getObservableBlockedList()

    private fun isFriend(uuid: UUID) = uuid in socialStateManager.relationshipStates.getObservableFriendList()

    fun handleBlockOrUnblock(uuid: UUID) {
        UUIDUtil.getName(uuid).thenAcceptOnMainThread {
            val block = !isBlocked(uuid)
            val blockText = if (block) {
                "Block"
            } else {
                "Unblock"
            }
            GuiUtil.pushModal { manager ->
                DangerConfirmationEssentialModal(manager, blockText, false).configure {
                    titleText = "Are you sure you want to ${blockText.lowercase()} $it?"
                }.onPrimaryAction {
                    if (block) {
                        socialStateManager.relationshipStates.blockPlayer(uuid, true)
                    } else {
                        socialStateManager.relationshipStates.unblockPlayer(uuid, true)
                    }
                }
            }
        }
    }

    fun handleAddOrRemove(uuid: UUID) {
        UUIDUtil.getName(uuid).thenAcceptOnMainThread {
            if (isFriend(uuid)) {
                GuiUtil.pushModal { manager ->
                    DangerConfirmationEssentialModal(manager, "Remove", false).configure {
                        titleText = "Are you sure you want to remove $it as your friend?"
                    }.onPrimaryAction {
                        socialStateManager.relationshipStates.removeFriend(uuid, false)
                    }
                }
            } else {
                socialStateManager.relationshipStates.addFriend(uuid, false)
            }

        }
    }


    companion object {

        @JvmStatic
        fun getInstance(): SocialMenu? {
            return GuiUtil.openedScreen() as? SocialMenu
        }

        fun getGuiScaleOffset(): Float {
            return if (EssentialConfig.enlargeSocialMenuChatMetadata) {
                0f
            } else {
                -1f
            }
        }
    }

}
