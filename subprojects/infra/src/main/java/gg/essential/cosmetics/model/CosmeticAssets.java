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
import gg.essential.model.EssentialAsset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CosmeticAssets {

    @SerializedName("a")
    private final @NotNull EssentialAsset thumbnail;

    @SerializedName("b")
    private final @Nullable EssentialAsset texture;

    @SerializedName("c")
    private final @NotNull CosmeticGeometry geometry;

    @SerializedName("d")
    private final @Nullable EssentialAsset animations;

    @SerializedName("e")
    private final @Nullable CosmeticSkinMask skinMask;

    @SerializedName("f")
    private final @Nullable EssentialAsset settings;

    public CosmeticAssets(
            final @NotNull EssentialAsset thumbnail,
            final @Nullable EssentialAsset texture,
            final @NotNull CosmeticGeometry geometry,
            final @Nullable EssentialAsset animations,
            final @Nullable CosmeticSkinMask skinMask,
            final @Nullable EssentialAsset settings
    ) {
        this.thumbnail = thumbnail;
        this.texture = texture;
        this.geometry = geometry;
        this.animations = animations;
        this.skinMask = skinMask;
        this.settings = settings;
    }

    public @NotNull EssentialAsset getThumbnail() {
        return this.thumbnail;
    }

    public @Nullable EssentialAsset getTexture() {
        return this.texture;
    }

    public @NotNull CosmeticGeometry getGeometry() {
        return this.geometry;
    }

    public @Nullable EssentialAsset getAnimations() {
        return this.animations;
    }

    public @Nullable CosmeticSkinMask getSkinMask() {
        return this.skinMask;
    }

    public @Nullable EssentialAsset getSettings() {
        return this.settings;
    }

}
