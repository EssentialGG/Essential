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
package gg.essential.gui.friends.previews

import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.Message
import gg.essential.Essential
import gg.essential.config.LoadsResources
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.impl.commonmark.node.BlockQuote
import gg.essential.elementa.impl.commonmark.node.Node
import gg.essential.elementa.impl.commonmark.parser.Parser
import gg.essential.elementa.impl.commonmark.renderer.NodeRenderer
import gg.essential.elementa.impl.commonmark.renderer.text.TextContentNodeRendererContext
import gg.essential.elementa.impl.commonmark.renderer.text.TextContentRenderer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.not
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.Observer
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.friends.Tab
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.studio.Tag
import gg.essential.gui.util.hoveredState
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import okhttp3.HttpUrl
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random
import gg.essential.gui.elementa.state.v2.stateBy as stateByV2

class ChannelPreview(
    val gui: SocialMenu,
    val channel: Channel
) : UIBlock(), SearchableItem {
    val otherUser: UUID? = channel.getOtherUser()

    private val messengerStates = gui.socialStateManager.messengerStates
    private val latestMessageState = messengerStates.getLatestMessage(channel.id)

    private val uuid = otherUser ?: UUIDUtil.formatWithDashes("00000000000000000000000000000000")

    private val activity = gui.socialStateManager.statusStates.getActivityState(uuid)
    private val joinable = activity.map { it.isJoinable() }

    val titleState = if (otherUser != null) {
        UUIDUtil.nameState(otherUser)
    } else {
        messengerStates.getTitle(channel.id)
    }

    val hasUnreadState = messengerStates.getUnreadChannelState(channel.id)

    val isChannelMutedState = messengerStates.getMuted(channel.id)

    val latestMessageTimestamp = latestMessageState.map { ((it?.id ?: channel.id) shr 22) + MessageUtils.messageTimeEpocMillis }

    private val doShowTimestampState = hasUnreadState.not() and latestMessageState.map { it != null }

    init {
        val dropdownOpen = mutableStateOf(false)
        val active =
            gui.chatTab.currentMessageView.map { it?.preview == this } and gui.selectedTab.map { it == Tab.CHAT }
        val unreadQuantity = Tag(
            stateOf(EssentialPalette.RED),
            stateOf(EssentialPalette.TEXT_HIGHLIGHT),
            messengerStates.getNumUnread(channel.id).map { it.toString() },
        ) effect ShadowEffect(EssentialPalette.BLACK)
        val image = if (otherUser != null) {
            CachedAvatarImage.ofUUID(otherUser)
        } else {
            if (channel.isAnnouncement()) {
                EssentialPalette.ANNOUNCEMENT_ICON_8X.create()
            } else {
                newGroupIcon(channel.id)
            }
        }
        val color = Modifier.whenTrue(
            hoveredState().toV2() or dropdownOpen or active,
            Modifier.color(EssentialPalette.COMPONENT_BACKGROUND),
            Modifier.color(EssentialPalette.GUI_BACKGROUND),
        )
        val timestampState = memo {
            val lastMessageDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(latestMessageTimestamp()), ZoneId.systemDefault())

            val lastMessageDate = lastMessageDateTime.toLocalDate()
            val currentDate = LocalDate.now()

            when (ChronoUnit.DAYS.between(lastMessageDate, currentDate)) {
                0L -> formatTime(lastMessageDateTime, false)
                1L -> "Yesterday"
                in 2L..7L -> lastMessageDate.format(weekdayTimestampFormatter)
                else -> formatDate(lastMessageDate, lastMessageDate.year != currentDate.year)
            }
        }

        layoutAsBox(Modifier.fillWidth().height(40f).then(BasicYModifier(::SiblingConstraint)).then(color)) {
            row(Modifier.fillWidth(padding = 10f).fillHeight()) {
                image(Modifier.width(24f).heightAspect(1f))
                spacer(width = 7.5f)
                column(Modifier.fillRemainingWidth().fillHeight(), Arrangement.spacedBy(0f, FloatPosition.START), Alignment.Start) {
                    spacer(height = 10f)
                    row(Modifier.fillWidth()) {
                        box(Modifier.fillRemainingWidth()) {
                            text(titleState, truncateIfTooSmall = true, showTooltipForTruncatedText = false, modifier = Modifier.alignHorizontal(Alignment.Start))
                        }
                        if_(doShowTimestampState) {
                            spacer(width = 4f)
                            text(timestampState, shadow = false, modifier = Modifier.color(EssentialPalette.TEXT_DISABLED))
                        }
                    }
                    spacer(height = 5f)

                    fun LayoutScope.muteIndicator() {
                        row {
                            if_(isChannelMutedState and !hasUnreadState) {
                                image(EssentialPalette.MUTE_8X9, Modifier.color(EssentialPalette.TEXT_DISABLED))
                            }
                        }
                    }

                    if_(doShowTimestampState) {
                        row(Modifier.fillWidth(), Arrangement.SpaceBetween, Alignment.Start) {
                            description(Modifier.fillWidth(0.75f))
                            muteIndicator()
                        }
                    } `else` {
                        row(Modifier.fillWidth(), verticalAlignment = Alignment.Start) {
                            description(Modifier.fillRemainingWidth())
                            muteIndicator()
                        }
                    }
                }
                if_(hasUnreadState) {
                    spacer(width = 4f)
                    unreadQuantity(Modifier.childBasedWidth(padding = 2f).childBasedHeight(padding = 2f))
                }
            }
        }

        onRightClick {
            dropdownOpen.set(true)
            gui.showManagementDropdown(this@ChannelPreview, ContextOptionMenu.Position(it.absoluteX, it.absoluteY)) {
                dropdownOpen.set(false)
            }
        }

        onLeftClick {
            USound.playButtonPress()
            gui.openMessageScreen(this@ChannelPreview)
            it.stopPropagation()
        }
    }


    override fun getSearchTag() = titleState.get()

    inner class Description : UIContainer() {

        private val uuid = otherUser ?: UUIDUtil.formatWithDashes("00000000000000000000000000000000")

        private val activity = gui.socialStateManager.statusStates.getActivityState(uuid)
        private val joinable = activity.map { it.isJoinable() }

        private val descriptionState = latestMessageState.map { message ->
            val msg = message?.contents
                ?: if (channel.isAnnouncement()) {
                    "There are no announcements"
                } else {
                    "Click to send a message!"
                }

            markdownRenderer.render(
                Parser.builder()
                    .build()
                    .parse(msg)
            ).split("\n")[0]; // stop at new line
        }

        private val friendStatus by FriendStatus(uuid, gui.socialStateManager.statusStates).bindParent(this, joinable)

        private val descriptionText by EssentialUIText(
            shadow = false,
            shadowColor = EssentialPalette.BLACK,
            truncateIfTooSmall = true,
            showTooltipForTruncatedText = false,
        ).bindText(descriptionState.toV1(this)).constrain {
            width = width.coerceAtMost(100.percent)
            color = EssentialPalette.TEXT_DISABLED.toConstraint()
        }.bindParent(this, !joinable)

        init {
            constrain {
                height = ChildBasedSizeConstraint()
            }

            Modifier.whenTrue(
                doShowTimestampState,
                Modifier.fillWidth(0.75f),
                Modifier.fillRemainingWidth()
            ).applyToComponent(this)
        }
    }

    companion object {
        private val groupIcons = listOf("blue", "purple", "red", "yellow")

        @LoadsResources("/assets/essential/textures/friends/group_[a-z]+.png")
        fun newGroupIcon(channelId: Long): UIImage {
            val name = groupIcons.random(Random(channelId))
            return UIImage.ofResourceCached("/assets/essential/textures/friends/group_$name.png")
        }

        private val markdownRenderer = TextContentRenderer.builder()
            .stripNewlines(false)
            .nodeRendererFactory(::PlainBlockQuoteNodeRenderer)
            .build()

        private val weekdayTimestampFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
    }

    /**
     * Renders BlockQuotes plainly instead of using guillemets like `CoreTextContentNodeRenderer`
     */
    class PlainBlockQuoteNodeRenderer(val context: TextContentNodeRendererContext) : NodeRenderer {
        private val content = context.writer

        override fun getNodeTypes(): Set<Class<out Node>> =
            setOf(BlockQuote::class.java)


        override fun render(node: Node) {
            content.write(">")
            visitChildren(node)

            if (node.next != null) {
                content.line()
            }
        }

        private fun visitChildren(parent: Node) {
            var node = parent.firstChild
            while (node != null) {
                context.render(node)
                node = node.next
            }
        }
    }

    private fun LayoutScope.description(modifier: Modifier) {
        box(modifier) {
            if_(joinable) {
                FriendStatus(uuid, gui.socialStateManager.statusStates)()
            } `else` {

                bind(latestMessageState) {

                    val (icon, text) = getDescriptionContent()
                    val descriptionModifier = Modifier.color(EssentialPalette.TEXT_DISABLED).alignHorizontal(Alignment.Start)

                    row(Modifier.fillWidth().alignHorizontal(Alignment.Start), Arrangement.spacedBy(5f), Alignment.End) {
                        if (icon != null) {
                            icon.create()(descriptionModifier)
                        }

                        box(Modifier.fillRemainingWidth()) {
                            text(
                                text.toV1(this@ChannelPreview),
                                descriptionModifier,
                                shadow = false,
                                truncateIfTooSmall = true,
                                showTooltipForTruncatedText = false,
                            )
                        }

                    }

                }

            }

        }

    }

    private fun getDescriptionContent(): Pair<ImageFactory?, State<String>> {
        val message = latestMessageState.get() ?: return Pair(
            null,
            stateOf(if (channel.isAnnouncement()) {
                "There are no announcements"
            } else {
                "Click to send a message!"
            })
        )
        val content = message.contents

        val urlMatches = MessageUtils.URL_REGEX.findAll(content)
        val onlyUrls = urlMatches.count() > 0 && content.replace(MessageUtils.URL_REGEX, "").isBlank()

        val inviteMatch = MessageUtils.INVITE_URL_REGEX.find(content)
        val skinMatch = MessageUtils.SKIN_URL_REGEX.find(content)
        val giftMatch = MessageUtils.GIFT_URL_REGEX.find(content)
        if (onlyUrls) {
            return when {
                MessageUtils.SCREENSHOT_URL_REGEX.find(content) != null ->
                    Pair(EssentialPalette.PICTURES_SHORT_9X7, State { pictureDescription(message) })
                inviteMatch != null ->
                    Pair(EssentialPalette.ENVELOPE_9X7, inviteDescription(inviteMatch.value))
                skinMatch != null ->
                    Pair(EssentialPalette.PERSON_4X6, stateOf("Shared a skin"))
                giftMatch != null ->
                    Pair(EssentialPalette.WARDROBE_GIFT_7X, giftDescription(giftMatch.value))
                false ->
                    Pair(EssentialPalette.COSMETICS_10X7, stateOf("Shared an outfit")) // TODO: Add outfit message condition
                else ->
                    Pair(EssentialPalette.LINK_8X7, textDescription(message))
            }
        }

        return Pair(null, textDescription(message))
    }

    private fun textDescription(message: Message): State<String> {
        return stateOf(markdownRenderer.render(
            Parser.builder()
                .build()
                .parse(message.contents)
        ).split("\n")[0]) // stop at new line
    }

    private fun Observer.pictureDescription(message: Message): String {
        var numberOfPictures = 0
        for (loopMessage in messengerStates.getMessageListState(channel.id)()
            .sortedByDescending { MessageUtils.getSentTimeStamp(it.id) }) {
            if (message.sender != loopMessage.sender) {
                break
            }
            val numberOfScreenshotsInMessage =
                MessageUtils.SCREENSHOT_URL_REGEX.findAll(loopMessage.contents).count()
            if (numberOfScreenshotsInMessage == 0) {
                break
            }
            numberOfPictures += numberOfScreenshotsInMessage
        }
        return "$numberOfPictures Picture" + (if (numberOfPictures == 1) "" else "s")
    }

    private fun inviteDescription(inviteUrl: String): State<String> {
        val url = HttpUrl.parse(inviteUrl) ?: return stateOf("Invite")
        val name = url.queryParameter("name")
        if (name != null) {
            return stateOf(name)
        }

        val address = url.pathSegments().getOrNull(1) ?: return stateOf("Invite")
        val host = Essential.getInstance().connectionManager.spsManager.getHostFromSpsAddress(address)
            ?: return stateOf(address)

        return stateByV2 {
            val username = UUIDUtil.nameState(host)()
            if (username.isNotBlank()) "$username's World" else "Invite"
        }
    }

    private fun giftDescription(giftUrl: String): State<String> {
        val url = HttpUrl.parse(giftUrl) ?: return stateOf("Gift")
        val cosmeticId = url.pathSegments()[1] ?: return stateOf("Gift")
        val cosmetic = Essential.getInstance().connectionManager.cosmeticsManager.getCosmetic(cosmeticId) ?: return stateOf("Gift")

        return stateOf("Gift: ${cosmetic.displayName}")
    }
}
