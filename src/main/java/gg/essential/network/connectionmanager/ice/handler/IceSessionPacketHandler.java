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
package gg.essential.network.connectionmanager.ice.handler;

import gg.essential.connectionmanager.common.packet.ice.IceSessionPacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.ice.IceManager;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.upnp.model.UPnPSession;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class IceSessionPacketHandler extends PacketHandler<IceSessionPacket> {

    @NotNull
    private final IceManager iceManager;
    @NotNull
    private final SPSManager spsManager;

    public IceSessionPacketHandler(@NotNull IceManager iceManager, @NotNull SPSManager spsManager) {
        this.iceManager = iceManager;
        this.spsManager = spsManager;
    }

    @Override
    protected void onHandle(@NotNull ConnectionManager connectionManager, @NotNull IceSessionPacket packet) {
        UUID user = packet.getUser();

        UPnPSession localSession = this.spsManager.getLocalSession();
        if (localSession != null) {
            if (!localSession.getInvites().contains(user)) {
                return; // they are not invited, ignore them
            }

            // They are invited, setup the agent so we can accept their connection
            this.iceManager.createServerAgent(user, packet.getUfrag(), packet.getPassword());
        } else {
            IceManager.IceConnection connection = this.iceManager.getConnection(user);
            if (connection == null) {
                return;
            }
            connection.setRemoteCredentials(packet.getUfrag(), packet.getPassword());
        }
    }
}
