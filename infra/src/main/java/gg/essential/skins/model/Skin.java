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
package gg.essential.skins.model;

import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.skins.SkinModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Skin {

    private final @NotNull String id;

    private final @NotNull String name;

    private final @NotNull SkinModel model;

    private final @NotNull String hash;

    @SerializedName("created_at")
    private final @NotNull DateTime createdAt;

    @SerializedName("favorited_at")
    private final @Nullable DateTime favoritedAt;

    @SerializedName("last_used_at")
    private final @Nullable DateTime lastUsedAt;

    public Skin(@NotNull String id, @NotNull String name, @NotNull SkinModel model, @NotNull String hash, @NotNull DateTime createdAt, @Nullable DateTime favoritedAt, @Nullable DateTime lastUsedAt) {
        this.id = id;
        this.name = name;
        this.model = model;
        this.hash = hash;
        this.createdAt = createdAt;
        this.favoritedAt = favoritedAt;
        this.lastUsedAt = lastUsedAt;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull SkinModel getModel() {
        return model;
    }

    public @NotNull String getHash() {
        return hash;
    }

    public @NotNull DateTime getCreatedAt() {
        return createdAt;
    }

    public @Nullable DateTime getFavoritedAt() {
        return favoritedAt;
    }

    public @Nullable DateTime getLastUsedAt() {
        return lastUsedAt;
    }
}
