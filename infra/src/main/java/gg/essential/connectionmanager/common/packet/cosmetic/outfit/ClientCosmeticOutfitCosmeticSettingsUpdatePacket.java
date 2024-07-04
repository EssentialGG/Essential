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

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.cosmetics.model.CosmeticSetting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClientCosmeticOutfitCosmeticSettingsUpdatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String outfitId;

    @SerializedName("b")
    @NotNull
    private final String cosmeticId;

    @SerializedName("c")
    @NotNull
    private final List<CosmeticSetting> settings;

    public ClientCosmeticOutfitCosmeticSettingsUpdatePacket(
            @NotNull final String outfitId, @NotNull final String cosmeticId,
            @NotNull final List<CosmeticSetting> settings
    ) {
        this.outfitId = outfitId;
        this.cosmeticId = cosmeticId;
        this.settings = settings;
    }

    @NotNull
    public String getOutfitId() {
        return outfitId;
    }

    @NotNull
    public String getCosmeticId() {
        return cosmeticId;
    }

    @NotNull
    public List<CosmeticSetting> getSettings() {
        return settings;
    }

}
