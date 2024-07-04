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
package gg.essential.connectionmanager.common.packet.cosmetic.outfit;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.cosmetics.CosmeticSlot;
import gg.essential.cosmetics.model.CosmeticSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerCosmeticOutfitSelectedResponsePacket extends Packet {

    @NotNull
    private final UUID uuid;

    @Nullable
    private final String skinTexture;

    @Nullable
    private final Map<CosmeticSlot, String> equippedCosmetics;

    @Nullable
    private final Map<String, List<CosmeticSetting>> cosmeticSettings;

    public ServerCosmeticOutfitSelectedResponsePacket(
            @NotNull final UUID uuid, @Nullable final String skinTexture,
            @Nullable final Map<CosmeticSlot, String> equippedCosmetics,
            @Nullable final Map<String,  List<CosmeticSetting>> cosmeticSettings
    ) {
        this.uuid = uuid;
        this.skinTexture = skinTexture;
        this.equippedCosmetics = equippedCosmetics;
        this.cosmeticSettings = cosmeticSettings;
    }

    @NotNull
    public UUID getUUID() {
        return this.uuid;
    }

    @Nullable
    public String getSkinTexture() {
        return this.skinTexture;
    }

    @Nullable
    public Map<CosmeticSlot, String> getEquippedCosmetics() {
        return this.equippedCosmetics;
    }

    @Nullable
    public Map<String,  List<CosmeticSetting>> getCosmeticSettings() {
        return this.cosmeticSettings;
    }

}
