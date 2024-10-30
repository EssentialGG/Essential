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
package gg.essential.connectionmanager.common.model.serverdiscovery;

import gg.essential.holder.DisplayNameHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class Server implements DisplayNameHolder {
    private final @NotNull String id;
    private final @NotNull Map<String, String> names;
    private final @NotNull List<String> addresses;
    private final @NotNull List<Integer> protocolVersions;
    private final @NotNull String recommendedVersion;

    public Server(
        @NotNull String id, @NotNull Map<String, String> names, @NotNull List<String> addresses,
        @NotNull List<Integer> protocolVersions, boolean isFeatured, @NotNull String recommendedVersion
    ) {
        this.id = id;
        this.names = names;
        this.addresses = addresses;
        this.protocolVersions = protocolVersions;
        this.recommendedVersion = recommendedVersion;
    }

    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull Map<String, String> getDisplayNames() {
        return names;
    }

    public @NotNull List<String> getAddresses() {
        return addresses;
    }

    public @NotNull List<Integer> getProtocolVersions() {
        return protocolVersions;
    }

    public @NotNull String getRecommendedVersion() {
        return recommendedVersion;
    }
}
