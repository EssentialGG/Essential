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
package gg.essential.connectionmanager.common.packet.cosmetic.emote;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientCosmeticEmoteWheelUpdatePacket extends Packet {

    @SerializedName("a")
    private final String id;

    @SerializedName("b")
    private final int index;

    @SerializedName("c")
    private final @Nullable String value;

    public ClientCosmeticEmoteWheelUpdatePacket(
            final @NotNull String id,
            final int index,
            final @Nullable String value
    ) {
        this.id = id;
        this.index = index;
        this.value = value;
    }

    public String id() {
        return id;
    }

    public int index() {
        return index;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "ClientCosmeticEmoteWheelUpdatePacket{" +
                "id='" + id + '\'' +
                ", index=" + index +
                ", value='" + value + '\'' +
                '}';
    }

}
