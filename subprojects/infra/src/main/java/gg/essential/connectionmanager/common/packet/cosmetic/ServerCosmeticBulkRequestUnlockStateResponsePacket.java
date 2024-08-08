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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class ServerCosmeticBulkRequestUnlockStateResponsePacket extends Packet {

    private final @NotNull @SerializedName("unlock_states") Map<UUID, Boolean> unlockStates;

    public ServerCosmeticBulkRequestUnlockStateResponsePacket(final @NotNull Map<UUID, Boolean> unlockStates) {
        this.unlockStates = unlockStates;
    }

    public @NotNull Map<UUID, Boolean> getUnlockStates() {
        return Collections.unmodifiableMap(this.unlockStates);
    }

}