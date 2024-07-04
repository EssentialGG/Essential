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
package gg.essential.connectionmanager.common.packet.upnp;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.upnp.UPnPPrivacy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientUPnPSessionCreatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String ip;

    @SerializedName("b")
    private final int port;

    @SerializedName("c")
    @NotNull
    private final UPnPPrivacy privacy;

    private final @Nullable @SerializedName("d") Integer protocolVersion;

    private final @Nullable @SerializedName("e") String worldName;

    public ClientUPnPSessionCreatePacket(@NotNull final String ip, final int port, @NotNull final UPnPPrivacy privacy, final @Nullable Integer protocolVersion, final @Nullable String worldName) {
        this.ip = ip;
        this.port = port;
        this.privacy = privacy;
        this.protocolVersion = protocolVersion;
        this.worldName = worldName;
    }

    @NotNull
    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    @NotNull
    public UPnPPrivacy getPrivacy() {
        return this.privacy;
    }

    public @Nullable Integer getProtocolVersion() {
        return this.protocolVersion;
    }

    public @Nullable String getWorldName() {
        return this.worldName;
    }

}
