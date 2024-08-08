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
package gg.essential.model;

import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class EssentialAsset {

    @SerializedName(value = "a", alternate = "url")
    @NotNull
    private final String url;

    @SerializedName(value = "b", alternate = "checksum")
    @NotNull
    private final String checksum;

    public EssentialAsset(@NotNull final String url, @NotNull final String checksum) {
        this.url = url;
        this.checksum = checksum;
    }

    @NotNull
    public String getUrl() {
        return this.url;
    }

    @NotNull
    public String getChecksum() {
        return this.checksum;
    }

}
