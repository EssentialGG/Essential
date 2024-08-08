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
import com.sparkuniverse.toolbox.relationships.enums.RelationshipType;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ClientRelationshipCreatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final UUID uuid;

    @SerializedName("b")
    @NotNull
    private final RelationshipType type;

    public ClientRelationshipCreatePacket(@NotNull final UUID uuid, @NotNull final RelationshipType type) {
        this.uuid = uuid;
        this.type = type;
    }

    @NotNull
    public UUID getUUID() {
        return this.uuid;
    }

    @NotNull
    public RelationshipType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "ClientRelationshipCreatePacket{" +
                "uuid=" + uuid +
                ", type=" + type +
                '}';
    }

}
