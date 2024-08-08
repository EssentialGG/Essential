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
package gg.essential.connectionmanager.common.packet.relationships;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

public class ServerRelationshipCreateFailedResponsePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String reason;

    public ServerRelationshipCreateFailedResponsePacket(@NotNull final String reason) {
        this.reason = reason;
    }

    @NotNull
    public String getReason() {
        return this.reason;
    }

}
