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
package gg.essential.connectionmanager.common.packet.relationships.privacy;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.relationships.enums.FriendRequestPrivacySetting;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FriendRequestPrivacySettingPacket extends Packet {

    @NotNull
    @SerializedName("a")
    private final FriendRequestPrivacySetting setting;

    public FriendRequestPrivacySettingPacket(@NotNull final FriendRequestPrivacySetting setting) {
        this.setting = setting;
    }

    @NotNull
    public FriendRequestPrivacySetting getSetting() {
        return this.setting;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return this.setting == ((FriendRequestPrivacySettingPacket) o).setting;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.setting);
    }

    @Override
    public String toString() {
        return "FriendRequestPrivacySettingUpdatePacket{" +
                "setting=" + this.setting +
                '}';
    }

}
