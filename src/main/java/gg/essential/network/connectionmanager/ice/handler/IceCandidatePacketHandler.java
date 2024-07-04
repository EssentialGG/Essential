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

import gg.essential.connectionmanager.common.packet.ice.IceCandidatePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.ice.IceManager;
import org.jetbrains.annotations.NotNull;

public class IceCandidatePacketHandler extends PacketHandler<IceCandidatePacket> {

    @NotNull
    private final IceManager iceManager;

    public IceCandidatePacketHandler(@NotNull IceManager iceManager) {
        this.iceManager = iceManager;
    }

    @Override
    protected void onHandle(@NotNull ConnectionManager connectionManager, @NotNull IceCandidatePacket packet) {
        this.iceManager.addRemoteCandidate(packet.getUser(), packet.getCandidate());
    }
}
