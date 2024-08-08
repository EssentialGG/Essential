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
package gg.essential.connectionmanager.common.packet.cosmetic;


import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Used by the client to check if the specified target users have unlocked the given cosmetic or not.
 */
public class ClientCosmeticBulkRequestUnlockStatePacket extends Packet {

    private final @NotNull @SerializedName("target_user_ids") Set<UUID> targetUserIds;

    private final @NotNull @SerializedName("cosmetic_id") String cosmeticId;

    public ClientCosmeticBulkRequestUnlockStatePacket(final @NotNull Set<UUID> targetUserIds, final @NotNull String cosmeticId) {
        this.targetUserIds = targetUserIds;
        this.cosmeticId = cosmeticId;
    }

    public @NotNull String getCosmeticId() {
        return this.cosmeticId;
    }

}