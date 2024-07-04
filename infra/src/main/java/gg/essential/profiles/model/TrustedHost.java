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
package gg.essential.profiles.model;

import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class TrustedHost {

    @SerializedName(value = "a", alternate = {"id"})
    @NotNull
    private final String id;

    @SerializedName(value = "b", alternate = {"name"})
    @NotNull
    private final String name;

    @SerializedName(value = "c", alternate = {"domains"})
    @NotNull
    private final Set<String> domains;

    @SerializedName(value = "d", alternate = {"profileId"})
    @Nullable
    private final UUID profileId;

    public TrustedHost(
            @NotNull final String id, @NotNull final String name, @NotNull final Set<String> domains,
            @Nullable final UUID profileId
    ) {
        this.id = id;
        this.name = name;
        this.domains = domains;
        this.profileId = profileId;
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public Set<String> getDomains() {
        return this.domains;
    }

    @Nullable
    public UUID getProfileId() {
        return this.profileId;
    }

}
