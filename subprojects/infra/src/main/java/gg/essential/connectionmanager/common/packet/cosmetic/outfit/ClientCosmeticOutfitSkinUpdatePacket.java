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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientCosmeticOutfitSkinUpdatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String outfitId;

    @SerializedName("b")
    @Nullable
    private final String skinTexture;

    @SerializedName("c")
    @Nullable
    private final String skinId;

    public ClientCosmeticOutfitSkinUpdatePacket(@NotNull final String outfitId, @Nullable final String skinTexture, @Nullable final String skinId) {
        this.outfitId = outfitId;
        this.skinTexture = skinTexture;
        this.skinId = skinId;
    }

    @NotNull
    public String getOutfitId() {
        return this.outfitId;
    }

    @Nullable
    public String getSkinTexture() {
        return this.skinTexture;
    }

    @Nullable
    public String getSkinId() {
        return this.skinId;
    }
}
