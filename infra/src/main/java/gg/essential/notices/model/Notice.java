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
package gg.essential.notices.model;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.notices.NoticeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Notice {

    @SerializedName("a")
    private final @NotNull String id;

    @SerializedName("b")
    private final @NotNull NoticeType type;

    @SerializedName("c")
    private final @NotNull Map<@NotNull String, @NotNull Object> metadata;

    @SerializedName("d")
    private final boolean dismissible;

    @SerializedName("e")
    private final @Nullable DateTime activeAfter;

    @SerializedName("f")
    private final @Nullable DateTime expiresAt;

    public Notice(
            final @NotNull String id,
            final @NotNull NoticeType type,
            final @NotNull Map<@NotNull String, @NotNull Object> metadata,
            final boolean dismissible,
            final @Nullable DateTime activeAfter,
            final @Nullable DateTime expiresAt
    ) {
        this.id = id;
        this.type = type;
        this.metadata = metadata;
        this.dismissible = dismissible;
        this.activeAfter = activeAfter;
        this.expiresAt = expiresAt;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull NoticeType getType() {
        return this.type;
    }

    public @NotNull Map<@NotNull String, @NotNull Object> getMetadata() {
        return this.metadata;
    }

    public boolean isDismissible() {
        return this.dismissible;
    }

    public @Nullable DateTime getActiveAfter() {
        return this.activeAfter;
    }

    public @Nullable DateTime getExpiresAt() {
        return this.expiresAt;
    }

    @Override
    public String toString() {
        return "Notice{" +
                "id=" + this.id +
                ", type=" + this.type +
                ", metadata=" + this.metadata +
                ", dismissible=" + this.dismissible +
                ", activeAfter=" + this.activeAfter +
                ", expiresAt=" + this.expiresAt +
                '}';
    }

}
