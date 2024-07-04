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
import org.jetbrains.annotations.Nullable;

public class ClientUPnPSessionUpdatePacket extends Packet {

    @SerializedName("a")
    @Nullable
    private final String ip;

    @SerializedName("b")
    @Nullable
    private final Integer port;

    @SerializedName("c")
    @Nullable
    private final UPnPPrivacy privacy;

    public ClientUPnPSessionUpdatePacket(
            @Nullable final String ip, @Nullable final Integer port, @Nullable final UPnPPrivacy privacy
    ) {
        this.ip = ip;
        this.port = port;
        this.privacy = privacy;
    }

    @Nullable
    public String getIp() {
        return this.ip;
    }

    @Nullable
    public Integer getPort() {
        return this.port;
    }

    @Nullable
    public UPnPPrivacy getPrivacy() {
        return this.privacy;
    }

}
