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

public class ClientChatChannelMessageReadStatePacket extends Packet {

    @SerializedName("a")
    private final long channelId;

    @SerializedName("b")
    private final long messageId;

    @SerializedName("c")
    private final boolean state;

    public ClientChatChannelMessageReadStatePacket(final long channelId, final long messageId, final boolean state) {
        this.channelId = channelId;
        this.messageId = messageId;
        this.state = state;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getMessageId() {
        return messageId;
    }

    public boolean getState() {
        return this.state;
    }

}
