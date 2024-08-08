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
package gg.essential.connectionmanager.common.packet.chat;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

public class ClientChatChannelMessageUpdatePacket extends Packet {

    @SerializedName("a")
    private final long channelId;

    @SerializedName("b")
    private final long messageId;

    @SerializedName("c")
    private final String content;

    public ClientChatChannelMessageUpdatePacket(
            final long channelId, final long messageId, @NotNull final String content
    ) {
        this.channelId = channelId;
        this.messageId = messageId;
        this.content = content;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public long getMessageId() {
        return this.messageId;
    }

    @NotNull
    public String getContent() {
        return this.content;
    }

}
