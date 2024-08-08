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

import java.util.UUID;

public class ClosedInfo {

    @SerializedName("a")
    @NotNull
    private final DateTime at;

    @SerializedName("b")
    @NotNull
    private final UUID by;

    @SerializedName("c")
    @NotNull
    private final String reason;

    public ClosedInfo(@NotNull final DateTime at, @NotNull final UUID by, @NotNull final String reason) {
        this.at = at;
        this.by = by;
        this.reason = reason;
    }

    @NotNull
    public DateTime getAt() {
        return this.at;
    }

    @NotNull
    public UUID getBy() {
        return this.by;
    }

    @NotNull
    public String getReason() {
        return this.reason;
    }

}
