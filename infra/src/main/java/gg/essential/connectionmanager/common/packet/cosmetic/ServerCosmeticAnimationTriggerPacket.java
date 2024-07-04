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

import java.util.UUID;

public class ServerCosmeticAnimationTriggerPacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final UUID userId;

    @SerializedName("b")
    @NotNull
    private final CosmeticSlot cosmeticSlot;

    @SerializedName("c")
    @NotNull
    private final String triggerName;

    public ServerCosmeticAnimationTriggerPacket(
            @NotNull final UUID userId, @NotNull final CosmeticSlot cosmeticSlot, @NotNull final String triggerName
    ) {
        this.userId = userId;
        this.cosmeticSlot = cosmeticSlot;
        this.triggerName = triggerName;
    }

    @NotNull
    public UUID getUserId() {
        return this.userId;
    }

    @NotNull
    public CosmeticSlot getCosmeticSlot() {
        return this.cosmeticSlot;
    }

    @NotNull
    public String getTriggerName() {
        return this.triggerName;
    }

}
