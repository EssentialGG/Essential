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
import gg.essential.cosmetics.CosmeticSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientCosmeticOutfitEquippedCosmeticsUpdatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String outfitId;

    @SerializedName("b")
    @NotNull
    private final CosmeticSlot slot;

    @SerializedName("c")
    @Nullable
    private final String cosmeticId;

    public ClientCosmeticOutfitEquippedCosmeticsUpdatePacket(
            @NotNull final String outfitId, @NotNull final CosmeticSlot slot, @Nullable final String cosmeticId
    ) {
        this.outfitId = outfitId;
        this.slot = slot;
        this.cosmeticId = cosmeticId;
    }

    @NotNull
    public String getOutfitId() {
        return this.outfitId;
    }

    @NotNull
    public CosmeticSlot getSlot() {
        return this.slot;
    }

    @Nullable
    public String getCosmeticId() {
        return this.cosmeticId;
    }

}
