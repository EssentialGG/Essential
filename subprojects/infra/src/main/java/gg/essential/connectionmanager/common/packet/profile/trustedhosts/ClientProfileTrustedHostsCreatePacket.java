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
package gg.essential.connectionmanager.common.packet.profile.trustedhosts;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ClientProfileTrustedHostsCreatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final String name;

    @SerializedName("b")
    @NotNull
    private final Set<String> domains;

    public ClientProfileTrustedHostsCreatePacket(@NotNull final String name, @NotNull final Set<String> domains) {
        this.name = name;
        this.domains = domains;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public Set<String> getDomains() {
        return this.domains;
    }

}
