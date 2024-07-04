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

public class ServerCheckoutPartnerCodeDataPacket extends Packet {

    @SerializedName("partner_code")
    private final @NotNull String partnerCode;

    @SerializedName("partner_name")
    private final @NotNull String partnerName;

    private final boolean persist;


    public ServerCheckoutPartnerCodeDataPacket(@NotNull String partnerCode, @NotNull String partnerName, boolean persist) {
        this.partnerCode = partnerCode;
        this.partnerName = partnerName;
        this.persist = persist;
    }

    public @NotNull String getPartnerCode() {
        return partnerCode;
    }

    public @NotNull String getPartnerName() {
        return partnerName;
    }

    public boolean isPersist() {
        return persist;
    }
}
