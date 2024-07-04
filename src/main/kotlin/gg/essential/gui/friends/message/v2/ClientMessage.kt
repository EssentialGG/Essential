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

import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.Message
import gg.essential.Essential
import gg.essential.cosmetics.CosmeticId
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.friends.message.MessageUtils.handleMarkdownUrls
import gg.essential.mod.Model
import gg.essential.mod.Skin
import okhttp3.HttpUrl
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant
import java.util.*

data class ClientMessage(
    val id: Long,
    val channel: Channel,
    val sender: UUID,
    val contents: String,
    val sendState: SendState,
    val replyTo: MessageRef?,
    val lastEditTime: Long?,
) {
    val sendTime: Instant = Instant.ofEpochMilli((id shr 22) + MessageUtils.messageTimeEpocMillis)
    val sent = sendState == SendState.CONFIRMED
    val parts: List<Part> = buildList {
        val cleanedText = contents.handleMarkdownUrls()
        var strippedText = cleanedText

        fun stripUrl(url: String) {
            val bracketedUrl = "<$url>"
            strippedText = if (strippedText.indexOf(bracketedUrl) != -1) {
                strippedText.replace(bracketedUrl, "")
            } else {
                strippedText.replace(url, "")
            }
        }

        // Replace no embed urls with "" so they are not parsed for embeds
        MessageUtils.SCREENSHOT_URL_REGEX.findAll(MessageUtils.URL_NO_EMBED_REGEX.replace(cleanedText, "")).forEach {
            val match = it.value.removeSuffix(">")

            try {
                add(Part.Image(URL(match)))
                stripUrl(match)
            } catch (e: MalformedURLException) {
                Essential.logger.debug("Ignoring invalid URL:", e)
            }
        }

        MessageUtils.URL_REGEX.findAll(MessageUtils.URL_NO_EMBED_REGEX.replace(cleanedText, "")).forEach {
            val match = it.value.removeSuffix(">")

            HttpUrl.parse(match)?.let { url ->
                if (url.host() == "essential.gg" && url.pathSegments().size > 1) {
                    if (url.pathSegments()[0] == "gift") {
                        add(Part.Gift(url.pathSegments()[1]))
                        stripUrl(match)
                    } else if (url.pathSegments()[0] == "skin" && url.pathSegments().size > 2) {
                        add(Part.Skin(Skin(url.pathSegments()[2], Model.byVariantOrDefault(url.pathSegments()[1]))))
                        stripUrl(match)
                    }
                }
            }
        }

        if (strippedText.isNotBlank()) {
            add(0, Part.Text(MessageUtils.URL_NO_EMBED_REGEX.replace(cleanedText, "<$1>"))) // Strip outer < > from no embed urls
        }
    }

    fun getInfraInstance(): Message {
        return Essential.getInstance().connectionManager.chatManager.getMessageById(id) ?: Message(
            id,
            channel.id,
            sender,
            contents,
            true, // So the social menu doesn't try to mark this message as read
            replyTo?.messageId,
            lastEditTime,
        )
    }

    sealed interface Part {
        data class Text(val content: String) : Part
        data class Image(val url: URL) : Part
        data class Gift(val id: CosmeticId) : Part
        data class Skin(val skin: gg.essential.mod.Skin) : Part
    }

    companion object {

        /**
         * Creates a message from a network type, excluding the replyTo state.
         */
        fun fromNetwork(message: Message): ClientMessage {
            val chatManager = Essential.getInstance().connectionManager.chatManager
            return ClientMessage(
                message.id,
                chatManager.getChannel(message.channelId).get(),
                message.sender,
                message.contents,
                SendState.CONFIRMED,
                message.replyTargetId?.let {
                   MessageRef(message.channelId, it)
                },
                message.lastEditTime,
            )
        }

    }
}

enum class SendState {
    SENDING,
    CONFIRMED,
    FAILED,
}