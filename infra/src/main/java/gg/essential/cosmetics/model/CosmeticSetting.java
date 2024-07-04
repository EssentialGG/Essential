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
package gg.essential.cosmetics.model;

import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CosmeticSetting {

    @SerializedName(value = "a", alternate = { "id" })
    private final @Nullable String id;

    @SerializedName(value = "b", alternate = { "type" })
    private final @NotNull String type;

    @SerializedName(value = "c", alternate = { "enabled" })
    private final boolean enabled;

    @SerializedName(value = "d", alternate = { "data" })
    private final @NotNull Map<@NotNull String, @NotNull Object> data;

    public CosmeticSetting(
            final @Nullable String id,
            final @NotNull String type,
            final boolean enabled,
            final @NotNull Map<@NotNull String, @NotNull Object> data
    ) {
        this.id = id;
        this.type = type;
        this.enabled = enabled;
        this.data = data;
    }


    public @Nullable String getId() {
        return id;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull Map<@NotNull String, @NotNull Object> getData() {
        return this.data;
    }

    public <T> @Nullable T getData(final @NotNull String key) {
        return (T) this.data.get(key);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean hasData(final @NotNull String key) {
        return this.data.containsKey(key);
    }

    public <T> void setData(
            final @NotNull String key,
            final @NotNull T value
    ) {
        this.data.put(key, value);
    }

}
