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
package gg.essential.gui.friends.message.v2

//#if MC>10809
import net.minecraft.init.SoundEvents
//#else
//$$ import net.minecraft.util.ResourceLocation
//#endif
import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.chat.ClientChatChannelMessageUpdatePacket
import gg.essential.connectionmanager.common.packet.chat.ServerChatChannelMessagePacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.mapEach
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.elementa.state.v2.removeAll
import gg.essential.gui.elementa.state.v2.toList
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.elementa.state.v2.toSet
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.friends.Tab
import gg.essential.gui.friends.message.MessageInput
import gg.essential.gui.friends.message.MessageScreen
import gg.essential.gui.friends.message.MessageTitleBar
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.friends.previews.ChannelPreview
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.notification.Notifications
import gg.essential.network.connectionmanager.EarlyResponseHandler
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit


class ReplyableMessageScreen(
    override val preview: ChannelPreview
) : MessageScreen() {
    private val gui = preview.gui
    private val active =
        gui.chatTab.currentMessageView.map { it == this@ReplyableMessageScreen } and gui.selectedTab.map { it == Tab.CHAT }

    private val standardBar by MessageTitleBar(this, gui).bindParent(gui.titleBar, active)

    private val scroller by ScrollComponent(
        verticalScrollOpposite = true,
        scrollAcceleration = 2f
    ).constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = FillConstraint()
    } childOf this

    private val content by UIContainer().constrain {
        y = 0.pixels(alignOpposite = true)
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf scroller

    private val showEmptyText = mutableStateOf(true).apply {
        content.children.addObserver { _, _ ->
            set(content.children.none {
                it is MessageWrapper
            })
        }
    }

    private var messageInput: MessageInput? = null

    private val topSpacer by Spacer(height = 6.pixels) childOf content

    private val bottomSpacer by Spacer(height = 7.pixels) childOf content

    private val emptyText by EssentialUIText("Send a message to begin chatting!").constrain {
        x = CenterConstraint()
        y = 10.pixels
        color = EssentialPalette.TEXT.toConstraint()
    }.bindParent(scroller.children[0], showEmptyText)

    private val scrollCleanup: () -> Unit

    private var lastRequest = 0L
    private val cm = Essential.getInstance().connectionManager
    private val channel = preview.channel
    private var receivedAllMessages = false
    private val sendQueue = mutableListStateOf<ClientMessage>()

    private val baseMessageListState = gui.socialStateManager.messengerStates.getMessageListState(preview.channel.id)

    private val messageListState = memo {
        val list = baseMessageListState().toMutableList()
        val latestMessageTimestamp = list.maxOfOrNull { if (it.sent) it.sendTime.toEpochMilli() else 0 } ?: 0
        for (messageWithIndex in sendQueue().withIndex()) {
            val message = messageWithIndex.value
            if (list.none { it.id == message.id }) {
                list.add(
                    if (message.sendState == SendState.SENDING) {
                        message.copy(
                            id = (kotlin.math.max(latestMessageTimestamp, message.sendTime.toEpochMilli()) + (messageWithIndex.index + 1) - MessageUtils.messageTimeEpocMillis) shl 22
                        )
                    } else {
                        message
                    }
                )
            }
        }
        list
    }.toListState()

    private var addedUnreadDivider = false

    private val contentSortComparator = compareBy<UIComponent> {
        // Sort by spacer vs content
        when (it) {
            topSpacer -> 0
            bottomSpacer -> 2
            else -> 1
        }
    }.thenComparing { uiComponent: UIComponent ->
        // Then sort by send time
        when (val component = uiComponent as MessengerElement) {
            is MessageWrapper -> component.message.sendTime
            is Divider -> component.timeStamp
        }
    }.thenComparing { uiComponent: UIComponent ->
        // Then sort by type
        when (uiComponent as MessengerElement) {
            is MessageWrapper -> 1
            is Divider -> 0
        }
    }.thenComparing { uiComponent: UIComponent ->
        // Then sort by divider type
        when (uiComponent) {
            is UnreadDivider -> 0
            is DateDivider -> 1
            else -> 2
        }
    }
    init {
        if (!preview.channel.isAnnouncement()) {
            messageInput = MessageInput(preview.titleState, replyingTo, editingMessage, this, ::sendMessage) childOf this
        }

        gui.createRightDividerScroller(
            active.toV1(this),
            yPositionAndHeight = scroller,
            initializeToBottom = true,
        ).let { (component, cleanup) ->
            scroller.setVerticalScrollBarComponent(component, hideWhenUseless = true)
            scrollCleanup = cleanup
        }

        if (messageInput != null) {
            val isPickingScreenshots = messageInput!!.screenshotAttachmentManager.isPickingScreenshots
            scroller.onLeftClick {
                if (isPickingScreenshots.get()) {
                    isPickingScreenshots.set(false)
                }
            }.bindEffect(FadeEffect(EssentialPalette.GUI_BACKGROUND, 0.4f), isPickingScreenshots)
        }

        val messageDates = messageListState.mapEach {
            val systemDefault = ZoneId.systemDefault()
            it.sendTime.atZone(systemDefault).toLocalDate().atStartOfDay(systemDefault)
        }.toSet().toList()

        content.layout {
            forEach(messageDates) {
                DateDividerImpl(it.toInstant())()
            }
            forEach(messageListState) { message ->
                MessageWrapperImpl(message, this@ReplyableMessageScreen).apply {
                    parseComponents(message, this).forEach { addComponent(it) }
                }()
            }
        }

        // Do the percent state manually as the default is not useful for a scroller which dynamically adds content
        // This one fades in the gradient over 100 pixels scrolled.
        // Could be made into a more generic solution, but to do the same trick for the opposite side,
        // one must have the actual max offset of the scroller, which isn't accessible and duplicating the calculation code seems like a bad idea.
        val percentState = BasicState(0f)

        scroller.addScrollAdjustEvent(false) { _, _ ->
            percentState.set(1 - (scroller.verticalOffset / 100).coerceIn(0f, 1f))
        }

        scroller.createGradient(false, 30.pixels, percentState = percentState, heightState = scroller.getHeightState())

        messageListState.onSetValue(this) { _ ->
            content.children.sortWith(contentSortComparator)

            // Check if we need to add the unread divider based on the new messages received
            insertUnreadDivider()

            // Recalculate visibility of timestamps when messages are mutated
            recalculateTimestampVisibility()
        }

        val channelMessages = cm.chatManager.getMessages(channel.id)

        if (channelMessages == null) { // Even for announcements this will still be true first round since personal channel isn't requested until after type ANNOUNCEMENT
            cm.chatManager.retrieveRecentMessageHistory(channel.id, null)
        } else if (channelMessages.size < 50) {
            cm.chatManager.retrieveMessageHistory(
                channel.id,
                channelMessages.values.minByOrNull { it.getSentTimestamp() }?.id,
                null,
                50 - channelMessages.size,
                null
            )
        }

        (replyingTo.zip(editingMessage)).onChange(this) {
            // If the user is scrolled all the way down, no change to scrolling is needed
            if (scroller.verticalOffset == 0f) {
                return@onChange
            }

            // If the user is not scrolled all the way down, we need to adjust the scroll to avoid moving content
            val scrollAdjust = if (it.first == null && it.second == null) {
                -ReplyMessageInput.ACTION_BAR_HEIGHT
            } else {
                ReplyMessageInput.ACTION_BAR_HEIGHT
            }
            scroller.scrollTo(verticalOffset = scroller.verticalOffset + scrollAdjust, smoothScroll = false)
        }

        scroller.addScrollAdjustEvent(isHorizontal = false) { scrollPercentage, _ ->
            val time = System.currentTimeMillis()
            if (scrollPercentage < -0.9 && time - lastRequest > 1000L) {
                lastRequest = time
                requestMoreMessages()
            }
        }

        content.children.sortWith(contentSortComparator)
        recalculateTimestampVisibility()
        insertUnreadDivider()
    }

    /**
     * Inserts a new message divider at the transition from read to unread messages
     * if a new line divider is not already present.
     */
    private fun insertUnreadDivider() {
        if (addedUnreadDivider) {
            return
        }

        fun insertDividerAtInstant(instant: Instant) {
            content.findChildrenOfType<DateDivider>(true).forEach { it.unread.set(false) }
            val unreadDivider by UnreadDividerImpl(instant) childOf content
            content.children.sortWith(contentSortComparator)
            addedUnreadDivider = true
        }

        fun insertDividerAt(clientMessage: ClientMessage) {

            // If the message is right after a date divider, set the date divider as unread instead of showing
            // an additional "NEW" line before the date divider.
            content.children.indexOfFirst {
                it is MessageWrapper && it.message == clientMessage
            }.let { index ->
                val previousSibling = content.children.getOrNull(index - 1)
                if (previousSibling is DateDivider) {
                    previousSibling.unread.set(true)
                    addedUnreadDivider = true
                    return@insertDividerAt
                }
            }

            insertDividerAtInstant(clientMessage.sendTime)
        }

        val messengerStates = gui.socialStateManager.messengerStates

        // Insert at the oldest message
        val sortedMessages = messageListState.get().sortedBy { it.sendTime }

        if (sortedMessages.none { messengerStates.getUnreadMessageState(it.getInfraInstance()).getUntracked() }) {
            // There are no unread messages. All messages are already read, so we won't need to place any divider this
            // time.
            // In fact, we mustn't place any divider in the future because if we do, it's probably on a message that
            // was sent while the screen is open and while technically correct (they are new after all!), we don't want
            // that.
            addedUnreadDivider = true
            return
        }

        if (sortedMessages.isEmpty()) {
            return
        }

        // Account for edge case
        // 1. All messages are unread and the new line divider should appear at the top of the list
        // 2. There is only a single unread message in the channel
        val first = sortedMessages.first()
        if (messengerStates.getUnreadMessageState(first.getInfraInstance()).getUntracked() && receivedAllMessages) {
            insertDividerAt(first)
            return
        }

        sortedMessages.zipWithNext { current, next ->
            val currentUnread = messengerStates.getUnreadMessageState(current.getInfraInstance()).getUntracked()
            val nextUnread = messengerStates.getUnreadMessageState(next.getInfraInstance()).getUntracked()

            if (!currentUnread && nextUnread) {
                insertDividerAt(next)
                return
            }
        }

    }

    private fun requestMoreMessages() {
        val messages = cm.chatManager.getMessages(channel.id) ?: return

        cm.chatManager.retrieveMessageHistory(
            channel.id,
            messages.values.minOfOrNull { it.id } ?: return,
            null,
        ) {
            if (!it.isPresent) {
                return@retrieveMessageHistory
            }

            val receivedMessages = (it.get() as? ServerChatChannelMessagePacket)?.messages ?: return@retrieveMessageHistory

            // If there are no more messages in the channel, check if the first messages
            // is unread to insert the unread divider
            if (receivedMessages.isEmpty()) {
                receivedAllMessages = true
                insertUnreadDivider() // Manually call because the observer that calls this processes before we set receivedAllMessages
            }

        }
    }

    private fun recalculateTimestampVisibility() {
        // Timestamps are hidden if the message was sent by the same player,
        // within 60 seconds of the previous message, and within 5 minutes of the
        // latest message with a timestamp

        val chainStartDelta = TimeUnit.MINUTES.toMillis(5)
        var chainStartTime: Long? = null
        for (it in content.children) {
            if (it !is MessageWrapper) {
                chainStartTime = null
                continue
            }
            if (chainStartTime != null && it.previousSiblingIsSameSenderWithinAMinute() && (it.message.sendTime.toEpochMilli() - chainStartTime <= chainStartDelta)) {
                it.showTimestamp.set(false)
                continue
            }
            chainStartTime = it.message.sendTime.toEpochMilli()
            it.showTimestamp.set(true)
        }
    }

    private fun parseComponents(message: ClientMessage, messageWrapper: MessageWrapper): List<MessageLine> {
        val messages = mutableListOf<MessageLine>()
        for (part in message.parts) {
            messages.add(when (part) {
                is ClientMessage.Part.Gift -> GiftEmbedImpl(part.id, messageWrapper)
                is ClientMessage.Part.Image -> ImageEmbedImpl(part.url, messageWrapper)
                is ClientMessage.Part.Skin -> SkinEmbedImpl(part.skin, messageWrapper)
                is ClientMessage.Part.Text -> ParagraphLineImpl(messageWrapper, part.content)
            })
        }
        return messages
    }

    fun editMessage(message: String) {
        val originalMessage = editingMessage.get() ?: return
        editingMessage.set(null)

        if (message == originalMessage.contents) {
            return
        }

        cm.send(ClientChatChannelMessageUpdatePacket(originalMessage.channel.id, originalMessage.id, message)) { optionalPacket ->
            if ((optionalPacket.orElse(null) as? ResponseActionPacket)?.isSuccessful == true) {
                cm.chatManager.updateMessage(originalMessage, message)
            } else {
                Notifications.push("Error editing message", "An error occurred while editing your message")
            }
        }
    }

    fun sendMessage(message: String) {
        val replyingTo = replyingTo.get()
        this.replyingTo.set(null)

        if (message.isBlank())
            return

        scroller.scrollToBottom()

        //#if MC>10809
        USound.playSoundStatic(SoundEvents.BLOCK_NOTE_HAT, .25f, 0.75f)
        //#else
        //$$ USound.playSoundStatic(ResourceLocation("note.hat"), .25f, 0.75f)
        //#endif

        val connectionManager = Essential.getInstance().connectionManager

        val fakeMessage = ClientMessage(
            (System.currentTimeMillis() - MessageUtils.messageTimeEpocMillis) shl 22, // ID (and implied send time) may be updated by fakeOutgoingMessageTimestamps()
            preview.channel,
            UUIDUtil.getClientUUID(),
            message,
            SendState.SENDING,
            replyingTo?.let {
                MessageRef(preview.channel.id, it.id)
            },
            null,
        )
        sendMessage(fakeMessage)
    }

    private fun sendMessage(message: ClientMessage) {
        // Add to sendQueue, so we can track the order of messages
        // and manage time desync between client and server
        sendQueue.add(message)

        val trimmed = message.contents.trim().replace("`", "").replace("(?<!  )\\n".toRegex(), "  \n")

        Essential.getInstance().connectionManager.chatManager.sendMessage(message.channel.id, trimmed, message.replyTo?.messageId, EarlyResponseHandler { packet ->
            sendQueue.removeAll { it.id == message.id }

            if (packet.orElse(null) !is ServerChatChannelMessagePacket) {
                sendQueue.add(message.copy(sendState = SendState.FAILED))
            }
        })
    }

    override fun onClose() {
        standardBar.hide(instantly = true)
        scrollCleanup()
        messageInput?.cleanup()
    }

    override fun scrollToMessage(message: ClientMessage) {
        val component = content.children.find {
            it is MessageWrapper && it.message.id == message.id
        } as? MessageWrapper ?: return

        scroller.scrollToCenterComponent(component, smooth = true)

        if (editingMessage.get() == null) {
            component.flashHighlight()
        }
    }

    override fun retrySend(message: ClientMessage) {
        if (message.sendState != SendState.FAILED || message.sender != UUIDUtil.getClientUUID()) {
            throw IllegalArgumentException("Message was already sent or was not sent by the client")
        }
        sendQueue.removeAll { it.id == message.id }
        sendMessage(message.copy(sendState = SendState.SENDING))
    }

    override fun markedManuallyUnread(messageWrapper: MessageWrapper) {
        scroller.holdScrollVerticalLocation(messageWrapper) {
            addedUnreadDivider = false

            // Delete the existing unread divider if it exists
            content.children.find { it is UnreadDivider }?.hide(instantly = true)

            content.childrenOfType<MessageWrapperImpl>().filter {
                it.sendTime >= messageWrapper.sendTime && !it.sentByClient
            }.forEach {
                it.markSelfUnread()
            }

            // Add the unread message divider
            insertUnreadDivider()
        }

    }

}
