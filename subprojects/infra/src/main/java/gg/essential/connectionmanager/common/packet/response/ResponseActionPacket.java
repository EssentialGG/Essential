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
package gg.essential.connectionmanager.common.packet.response;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.Nullable;

public class ResponseActionPacket extends Packet {

    @SerializedName(value = "a", alternate = "successful")
    private final boolean successful;

    @SerializedName(value = "b", alternate = "error_message")
    private final @Nullable String errorMessage;

    public ResponseActionPacket(final boolean successful, @Nullable String errorMessage) {
        this.successful = successful;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "ResponseActionPacket{" +
                "successful=" + successful +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
