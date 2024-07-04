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
package gg.essential.network.connectionmanager.handler.cosmetics;

import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsSkinTexturePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import org.jetbrains.annotations.NotNull;

public class ServerCosmeticSkinTexturePacketHandler extends PacketHandler<ServerCosmeticsSkinTexturePacket> {

    @Override
    protected void onHandle(
        @NotNull final ConnectionManager connectionManager,
        @NotNull final ServerCosmeticsSkinTexturePacket packet
    ) {
        final String skinTexture = packet.getSkinTexture();
        if (skinTexture != null && skinTexture.contains(";")) {
            final String[] split = skinTexture.split(";");
            Essential.getInstance().getGameProfileManager()
                .updatePlayerSkin(packet.getUuid(), split[1], split[0].equals("1") ? "slim" : "default");
        }
    }

}
