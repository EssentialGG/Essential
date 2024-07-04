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
package gg.essential.connectionmanager.common.packet.profile;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.enums.ActivityType;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ServerProfileActivityPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final UUID uuid;

    @SerializedName("b")
    @Nullable
    private final ActivityType type;

    @SerializedName("c")
    @Nullable
    private final String metadata;

    public ServerProfileActivityPacket(
            @NotNull final UUID uuid, @Nullable final ActivityType type, @Nullable final String metadata
    ) {
        this.uuid = uuid;
        this.type = type;
        this.metadata = metadata;
    }

    @NotNull
    public UUID getUUID() {
        return this.uuid;
    }

    @Nullable
    public ActivityType getType() {
        return this.type;
    }

    @Nullable
    public String getMetadata() {
        return this.metadata;
    }

}
