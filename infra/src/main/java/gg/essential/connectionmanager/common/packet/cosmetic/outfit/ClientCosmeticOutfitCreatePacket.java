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
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ClientCosmeticOutfitCreatePacket extends Packet {
    @SerializedName("name")
    @NotNull
    private final String name;

    @SerializedName("skin_id")
    @NotNull
    private final String skinId;

    @SerializedName("equipped_cosmetics")
    @Nullable
    private final Map<CosmeticSlot, String> equippedCosmetics;

    @SerializedName("cosmetic_settings")
    @Nullable
    private final Map<String, List<CosmeticSetting>> cosmeticSettings;

    public ClientCosmeticOutfitCreatePacket(@NotNull final String name, @NotNull String skinId, @Nullable Map<CosmeticSlot, String> equippedCosmetics, @Nullable Map<String, List<CosmeticSetting>> cosmeticSettings) {
        this.name = name;
        this.skinId = skinId;
        this.equippedCosmetics = equippedCosmetics;
        this.cosmeticSettings = cosmeticSettings;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getSkinId() {
        return skinId;
    }

    @Nullable
    public Map<CosmeticSlot, String> getEquippedCosmetics() {
        return equippedCosmetics;
    }

    @Nullable
    public Map<String, List<CosmeticSetting>> getCosmeticSettings() {
        return cosmeticSettings;
    }


}
