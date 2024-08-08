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
package gg.essential.connectionmanager.common.packet.ice;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class IceSessionPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final UUID user;

    @SerializedName("b")
    @NotNull
    private final String ufrag;

    @SerializedName("c")
    @NotNull
    private final String password;

    public IceSessionPacket(@NotNull final UUID user, @NotNull final String ufrag, @NotNull final String password) {
        this.user = user;
        this.ufrag = ufrag;
        this.password = password;
    }

    @NotNull
    public UUID getUser() {
        return this.user;
    }

    @NotNull
    public String getUfrag() {
        return this.ufrag;
    }

    @NotNull
    public String getPassword() {
        return this.password;
    }

}
