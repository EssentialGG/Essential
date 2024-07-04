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
import gg.essential.cosmetics.CosmeticSlot;
import gg.essential.cosmetics.SkinLayer;
import gg.essential.cosmetics.holder.SkinLayersHolder;
import gg.essential.holder.DisplayNameHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CosmeticType implements DisplayNameHolder, SkinLayersHolder {

    @SerializedName("a")
    private final @NotNull String id;

    @SerializedName("b")
    private final @NotNull CosmeticSlot slot;

    @SerializedName("c")
    private final @NotNull Map<@NotNull String, @NotNull String> displayNames;

    @SerializedName("d")
    private final @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> skinLayers;

    public CosmeticType(
            final @NotNull String id,
            final @NotNull CosmeticSlot slot,
            final @NotNull Map<@NotNull String, @NotNull String> displayNames,
            final @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> skinLayers
    ) {
        this.id = id;
        this.slot = slot;
        this.displayNames = displayNames;
        this.skinLayers = skinLayers;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull CosmeticSlot getSlot() {
        return slot;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getDisplayNames() {
        return this.displayNames;
    }

    @Override
    public @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> getSkinLayers() {
        return this.skinLayers;
    }

}
