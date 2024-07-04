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

public class ReportVerdict {

    @SerializedName("a")
    @NotNull
    String by;

    @SerializedName("b")
    @NotNull
    DateTime at;

    @SerializedName("c")
    @Nullable
    String reason;

    @SerializedName("d")
    @NotNull
    com.sparkuniverse.toolbox.chat.enums.ReportVerdict verdict;

    public ReportVerdict(@Nullable String by, @Nullable DateTime at, @Nullable String reason, @Nullable com.sparkuniverse.toolbox.chat.enums.ReportVerdict verdict) {
        this.by = by;
        this.at = at;
        this.reason = reason;
        this.verdict = verdict;
    }

    @NotNull
    public String getBy() {
        return by;
    }

    @NotNull
    public DateTime getAt() {
        return at;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

}
