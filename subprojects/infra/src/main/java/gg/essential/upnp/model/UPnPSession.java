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
package gg.essential.upnp.model;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.upnp.UPnPPrivacy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class UPnPSession {

    @SerializedName("a")
    @NotNull
    private final UUID hostUUID;

    @SerializedName("b")
    @NotNull
    private String ip;

    @SerializedName("c")
    private int port;

    @SerializedName("d")
    @NotNull
    private UPnPPrivacy privacy;

    @SerializedName("e")
    @NotNull
    private final Set<UUID> invites;

    @SerializedName("f")
    @NotNull
    private final DateTime createdAt;

    private final @Nullable @SerializedName("g") Integer protocolVersion;
    private final @Nullable @SerializedName("h") String worldName;

    public UPnPSession(
            @NotNull final UUID hostUUID, @NotNull final String ip, int port, @NotNull final UPnPPrivacy privacy,
            @NotNull final Set<UUID> invites, @NotNull final DateTime createdAt,

            final @Nullable Integer protocolVersion,
            final @Nullable String worldName
    ) {
        this.hostUUID = hostUUID;
        this.ip = ip;
        this.port = port;
        this.privacy = privacy;
        this.invites = invites;
        this.createdAt = createdAt;

        this.protocolVersion = protocolVersion;
        this.worldName = worldName;
    }

    @NotNull
    public UUID getHostUUID() {
        return this.hostUUID;
    }

    @NotNull
    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    @NotNull
    public UPnPPrivacy getPrivacy() {
        return this.privacy;
    }

    @NotNull
    public Set<UUID> getInvites() {
        return this.invites;
    }

    @NotNull
    public DateTime getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable Integer getProtocolVersion() {
        return this.protocolVersion;
    }

    public @Nullable String getWorldName() {
        return this.worldName;
    }

    public void setIp(@NotNull final String ip) {
        this.ip = ip;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setPrivacy(@NotNull final UPnPPrivacy privacy) {
        this.privacy = privacy;
    }

}
