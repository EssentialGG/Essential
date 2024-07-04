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
package gg.essential.network.connectionmanager.handler.wardrobe;

import gg.essential.connectionmanager.common.packet.wardrobe.ServerWardrobeStoreBundlePacket;
import gg.essential.cosmetics.model.CosmeticStoreBundle;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import org.jetbrains.annotations.NotNull;

public class ServerWardrobeStoreBundlePacketHandler extends PacketHandler<ServerWardrobeStoreBundlePacket> {

    @Override
    protected void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final ServerWardrobeStoreBundlePacket packet) {
        final CosmeticsManager cosmeticsManager = connectionManager.getCosmeticsManager();

        for (@NotNull final CosmeticStoreBundle bundle : packet.getStoreBundles()) {
            cosmeticsManager.getInfraCosmeticsData().addBundle(bundle);
        }
    }
}
