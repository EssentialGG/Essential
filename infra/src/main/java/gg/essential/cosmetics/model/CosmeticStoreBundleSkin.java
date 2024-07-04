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

import gg.essential.skins.SkinModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CosmeticStoreBundleSkin {

    private final @NotNull SkinModel model;

    private final @NotNull String hash;

    private final @Nullable String name;

    public CosmeticStoreBundleSkin(@NotNull SkinModel model, @NotNull String hash, @Nullable String name) {
        this.model = model;
        this.hash = hash;
        this.name = name;
    }

    public @NotNull SkinModel getModel() {
        return model;
    }

    public @NotNull String getHash() {
        return hash;
    }

    public @Nullable String getName() {
        return name;
    }
}
