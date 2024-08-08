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
package gg.essential.connectionmanager.common.packet.pingproxy;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

public class ClientPingProxyPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String hostname;

    @SerializedName("b")
    private final int port;

    @SerializedName("c")
    private final int protocolVersion;

    public ClientPingProxyPacket(@NotNull final String hostname, final int port, final int protocolVersion) {
        this.hostname = hostname;
        this.port = port;
        this.protocolVersion = protocolVersion;
    }

    @NotNull
    public String getHostname() {
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

}
