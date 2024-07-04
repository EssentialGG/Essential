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
import gg.essential.connectionmanager.common.packet.cosmetic.outfit.ServerCosmeticOutfitSelectedResponsePacket;
import gg.essential.cosmetics.CosmeticSlot;
import gg.essential.cosmetics.model.CosmeticSetting;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gg.essential.network.cosmetics.ConversionsKt.*;

public class ServerCosmeticOutfitSelectedResponsePacketHandler extends PacketHandler<ServerCosmeticOutfitSelectedResponsePacket> {

    @Override
    protected void onHandle(
        @NotNull final ConnectionManager connectionManager,
        @NotNull final ServerCosmeticOutfitSelectedResponsePacket packet
    ) {
        final String skinTexture = packet.getSkinTexture();
        if (skinTexture != null && skinTexture.contains(";")) {
            final String[] split = skinTexture.split(";");
            Essential.getInstance().getGameProfileManager().updatePlayerSkin(packet.getUUID(), split[1], split[0].equals("1") ? "slim" : "default");
        }

        Map<CosmeticSlot, String> equippedCosmetics = packet.getEquippedCosmetics();
        Map<String, List<CosmeticSetting>> cosmeticSettings = packet.getCosmeticSettings();
        if (equippedCosmetics == null) equippedCosmetics = Collections.emptyMap();
        if (cosmeticSettings == null) cosmeticSettings = Collections.emptyMap();
        connectionManager.getCosmeticsManager().getEquippedCosmeticsManager().update(
            packet.getUUID(),
            slotsToMod(equippedCosmetics),
            settingsToModSetting(cosmeticSettings)
        );
    }

}
