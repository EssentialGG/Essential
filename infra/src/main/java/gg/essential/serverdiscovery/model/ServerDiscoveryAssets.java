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
import gg.essential.model.EssentialAsset;
import org.jetbrains.annotations.Nullable;

public class ServerDiscoveryAssets {

    @SerializedName("a")
    @Nullable
    private final EssentialAsset logo;

    @SerializedName("b")
    @Nullable
    private final EssentialAsset background;

    public ServerDiscoveryAssets(@Nullable final EssentialAsset logo, @Nullable final EssentialAsset background) {
        this.logo = logo;
        this.background = background;
    }

    @Nullable
    public EssentialAsset getBackground() {
        return this.background;
    }

}
