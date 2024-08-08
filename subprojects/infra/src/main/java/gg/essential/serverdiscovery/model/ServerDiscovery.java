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
package gg.essential.serverdiscovery.model;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.holder.DisplayNameHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ServerDiscovery implements DisplayNameHolder {

    @SerializedName("a")
    @NotNull
    private final String id;

    @SerializedName("b")
    @NotNull
    private final Map<String, String> displayNames;

    @SerializedName("c")
    @NotNull
    private final List<String> addresses;

    @SerializedName("d")
    @Nullable
    private final ServerDiscoveryAssets assets;

    @SerializedName("e")
    @NotNull
    private final List<Integer> protocolVersions;

    @SerializedName("f")
    @Nullable
    private final List<String> tags;

    public ServerDiscovery(
            @NotNull final String id, @NotNull final Map<String, String> displayNames,
            @NotNull final List<String> addresses, @Nullable final ServerDiscoveryAssets assets,
            @NotNull final List<Integer> protocolVersions, @Nullable final List<String> tags
    ) {
        this.id = id;
        this.displayNames = displayNames;
        this.addresses = addresses;
        this.assets = assets;
        this.protocolVersions = protocolVersions;
        this.tags = tags;
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    @Override
    public Map<String, String> getDisplayNames() {
        return this.displayNames;
    }

    @NotNull
    public List<String> getAddresses() {
        return this.addresses;
    }

    @Nullable
    public ServerDiscoveryAssets getAssets() {
        return this.assets;
    }

    @NotNull
    public List<Integer> getProtocolVersions() {
        return this.protocolVersions;
    }

    @Nullable
    public List<String> getTags() {
        return this.tags;
    }

}
