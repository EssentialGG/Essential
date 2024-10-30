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
package gg.essential.connectionmanager.common.model.knownserver;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class KnownServer {
    private final @NotNull String id;
    private final @NotNull Map<String, String> names;
    private final @NotNull List<String> addresses;

    public KnownServer(
            final @NotNull String id,
            final @NotNull Map<String, String> names,
            final @NotNull List<String> addresses
    ) {
        this.id = id;
        this.names = names;
        this.addresses = addresses;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull Map<String, String> getNames() {
        return names;
    }

    public @NotNull List<String> getAddresses() {
        return addresses;
    }
}
