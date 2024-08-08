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

public class ServerPingProxyResponsePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String rawJson;

    @SerializedName("b")
    private final long latency;

    @SerializedName("c")
    @NotNull
    private final String region;

    public ServerPingProxyResponsePacket(
            @NotNull final String rawJson, final long latency, @NotNull final String region
    ) {
        this.rawJson = rawJson;
        this.latency = latency;
        this.region = region;
    }

    @NotNull
    public String getRawJson() {
        return this.rawJson;
    }

    public long getLatency() {
        return this.latency;
    }

    @NotNull
    public String getRegion() {
        return this.region;
    }

}
