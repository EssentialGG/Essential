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
package com.sparkuniverse.toolbox.chat.model;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.util.DateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CreatedInfo {

    @SerializedName("a")
    @NotNull
    private final DateTime at;

    @SerializedName("b")
    @Nullable
    private final UUID by;

    public CreatedInfo(@NotNull final DateTime at, @Nullable final UUID by) {
        this.at = at;
        this.by = by;
    }

    @NotNull
    public DateTime getAt() {
        return this.at;
    }

    @Nullable
    public UUID getBy() {
        return this.by;
    }

}
