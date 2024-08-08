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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class ServerUPnPSessionRemovePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final Set<UUID> hostUUIDs;

    public ServerUPnPSessionRemovePacket(@NotNull final UUID hostUUID) {
        this(Collections.singleton(hostUUID));
    }

    public ServerUPnPSessionRemovePacket(@NotNull final Set<UUID> hostUUIDs) {
        this.hostUUIDs = hostUUIDs;
    }

    @NotNull
    public Set<UUID> getHostUUIDs() {
        return this.hostUUIDs;
    }

}
