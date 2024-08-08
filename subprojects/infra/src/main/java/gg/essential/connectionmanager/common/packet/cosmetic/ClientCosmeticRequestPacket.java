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

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** Used by the client to get cosmetic data for the specified cosmetic ids. */
public class ClientCosmeticRequestPacket extends Packet {

    @SerializedName("a")
    private final @Nullable Set<@NotNull String> cosmeticIds;

    @SerializedName("b")
    private final @Nullable Set<@NotNull Integer> packageIds;

    public ClientCosmeticRequestPacket(
            final @Nullable Set<@NotNull String> cosmeticIds,
            final @Nullable Set<@NotNull Integer> packageIds
    ) {
        this.cosmeticIds = cosmeticIds;
        this.packageIds = packageIds;
    }

    public @Nullable Set<@NotNull String> getCosmeticIds() {
        return this.cosmeticIds;
    }

}
