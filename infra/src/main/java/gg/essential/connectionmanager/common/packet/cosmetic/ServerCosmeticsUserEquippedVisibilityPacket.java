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

/**
 * Sent by the server to the player receiving the packet informing them of their current equipped cosmetic visibility
 * setting state.
 */
public class ServerCosmeticsUserEquippedVisibilityPacket extends Packet {

    @SerializedName("a")
    private final boolean state;

    public ServerCosmeticsUserEquippedVisibilityPacket(final boolean state) {
        this.state = state;
    }

    public boolean getState() {
        return this.state;
    }

}
