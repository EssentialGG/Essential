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
package gg.essential.connectionmanager.common.packet.checkout;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class ClientCheckoutCosmeticsPacket extends Packet {

    @SerializedName("cosmetic_ids")
    @NotNull
    private final Set<String> cosmeticIds;
    @SerializedName("gift_to")
    @Nullable
    private final UUID giftTo;

    public ClientCheckoutCosmeticsPacket(@NotNull final Set<String> cosmeticIds, final @Nullable UUID giftTo) {
        this.cosmeticIds = cosmeticIds;
        this.giftTo = giftTo;
    }

    @NotNull
    public Set<String> getCosmeticIds() {
        return this.cosmeticIds;
    }

    @Nullable
    public UUID getGiftTo() {
        return giftTo;
    }
}
