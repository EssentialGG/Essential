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
import org.jetbrains.annotations.Nullable;

public class ClientProfileActivityPacket extends Packet {

    @SerializedName("a")
    @Nullable
    private final ActivityType type;

    @SerializedName("c")
    @Nullable
    private final String metadata;

    public ClientProfileActivityPacket(@Nullable final ActivityType type, @Nullable final String metadata) {
        this.type = type;
        this.metadata = metadata;
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
