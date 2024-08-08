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
package gg.essential.connectionmanager.common.packet;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class Packet {

    protected transient UUID uniqueId;

    protected transient Packet fakeReplyPacket;
    protected transient int fakeReplyDelayMs;

    @Nullable
    public UUID getPacketUniqueId() {
        return this.uniqueId;
    }

    public Packet getFakeReplyPacket() {
        return this.fakeReplyPacket;
    }

    public int getFakeReplyDelayMs() {
        return this.fakeReplyDelayMs;
    }

    public void setUniqueId(@Nullable final UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

}
