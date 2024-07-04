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
package gg.essential.connectionmanager.common.packet.connection;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

public class ConnectionRegisterPacketTypeIdPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String className;

    @SerializedName("b")
    private final int packetId;

    public ConnectionRegisterPacketTypeIdPacket(@NotNull final String className, final int packetId) {
        this.className = className;
        this.packetId = packetId;
    }

    @NotNull
    public String getClassName() {
        return this.className;
    }

    public int getPacketId() {
        return this.packetId;
    }

}
