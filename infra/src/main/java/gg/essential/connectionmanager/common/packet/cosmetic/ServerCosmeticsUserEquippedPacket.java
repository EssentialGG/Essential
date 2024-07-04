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
import gg.essential.cosmetics.CosmeticSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/** Sent by the server to other players to inform them of another player's equipped cosmetics. */
public class ServerCosmeticsUserEquippedPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final UUID uuid;

    @SerializedName("b")
    @NotNull
    private final Map<CosmeticSlot, String> equipped;

    public ServerCosmeticsUserEquippedPacket(
            @NotNull final UUID uuid, @NotNull final Map<CosmeticSlot, String> equipped
    ) {
        this.equipped = equipped;
        this.uuid = uuid;
    }

    @NotNull
    public Map<CosmeticSlot, String> getEquipped() {
        return this.equipped;
    }

    @NotNull
    public UUID getUUID() {
        return this.uuid;
    }

}
