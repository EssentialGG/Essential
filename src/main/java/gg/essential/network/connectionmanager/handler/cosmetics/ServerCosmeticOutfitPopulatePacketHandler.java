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

import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ServerCosmeticOutfitPopulatePacket;
import gg.essential.mod.cosmetics.CosmeticOutfit;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.cosmetics.OutfitManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static gg.essential.network.cosmetics.ConversionsKt.outfitsToMod;

public class ServerCosmeticOutfitPopulatePacketHandler extends PacketHandler<ServerCosmeticOutfitPopulatePacket> {

    @Override
    public Runnable handleAsync(@NotNull ConnectionManager connectionManager, @NotNull ServerCosmeticOutfitPopulatePacket packet) {
        List<gg.essential.cosmetics.model.CosmeticOutfit> infraOutfits = packet.getOutfits();
        List<CosmeticOutfit> receivedOutfits = outfitsToMod(infraOutfits);
        return () -> {
            OutfitManager outfitManager = connectionManager.getOutfitManager();

            String selectedId = null;
            for (gg.essential.cosmetics.model.CosmeticOutfit infraOutfit : infraOutfits) {
                if (infraOutfit.isSelected()) {
                    selectedId = infraOutfit.getId();
                    break;
                }
            }

            outfitManager.populateOutfits(receivedOutfits);
            outfitManager.populateSelection(selectedId);
        };
    }

    @Override
    protected void onHandle(
        @NotNull final ConnectionManager connectionManager, @NotNull final ServerCosmeticOutfitPopulatePacket packet
    ) {
        throw new UnsupportedOperationException();
    }

}
