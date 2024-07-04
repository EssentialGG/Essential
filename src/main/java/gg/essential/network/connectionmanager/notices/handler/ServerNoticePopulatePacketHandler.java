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
package gg.essential.network.connectionmanager.notices.handler;

import gg.essential.connectionmanager.common.packet.notices.ServerNoticePopulatePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.notices.NoticesManager;
import org.jetbrains.annotations.NotNull;

public class ServerNoticePopulatePacketHandler extends PacketHandler<ServerNoticePopulatePacket> {

    @NotNull
    private final NoticesManager noticesManager;

    public ServerNoticePopulatePacketHandler(@NotNull final NoticesManager noticesManager) {
        this.noticesManager = noticesManager;
    }

    @Override
    protected void onHandle(
        @NotNull final ConnectionManager connectionManager, @NotNull final ServerNoticePopulatePacket packet
    ) {
        this.noticesManager.populateNotices(packet.getNotices());
    }

}
