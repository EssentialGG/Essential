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
package gg.essential.network.connectionmanager.handler.upnp;

import gg.essential.connectionmanager.common.packet.upnp.ServerUPnPSessionRemovePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.sps.SPSManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ServerUPnPSessionRemovePacketHandler extends PacketHandler<ServerUPnPSessionRemovePacket> {

    @Override
    protected void onHandle(
        @NotNull final ConnectionManager connectionManager, @NotNull final ServerUPnPSessionRemovePacket packet
    ) {
        final SPSManager spsManager = connectionManager.getSpsManager();

        for (UUID hostUUID : packet.getHostUUIDs()) {
            spsManager.removeRemoteSession(hostUUID);
        }
    }

}
