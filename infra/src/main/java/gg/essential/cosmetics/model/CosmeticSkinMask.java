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
import org.jetbrains.annotations.Nullable;

public class CosmeticSkinMask {

    @SerializedName("steve")
    private final @Nullable EssentialAsset steve;

    @SerializedName("alex")
    private final @Nullable EssentialAsset alex;

    public CosmeticSkinMask(final @Nullable EssentialAsset steve, final @Nullable EssentialAsset alex) {
        this.steve = steve;
        this.alex = alex;
    }

    public @Nullable EssentialAsset getSteve() {
        return this.steve;
    }

    public @Nullable EssentialAsset getAlex() {
        return this.alex;
    }

}
