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
package gg.essential.media.model;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.util.DateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Media {

    @SerializedName("a")
    private final @NotNull String id;

    @SerializedName("b")
    private final @Nullable String title;

    @SerializedName("c")
    private final @Nullable String description;

    @SerializedName("d")
    private final @NotNull Map<String, MediaVariant> variants;

    @SerializedName("e")
    private final @NotNull MediaMetadata metadata;

    @SerializedName("f")
    private final @NotNull DateTime createdAt;

    public Media(
            final @NotNull String id,
            final @Nullable String title,
            final @Nullable String description,
            final @NotNull Map<String, MediaVariant> variants,
            final @NotNull MediaMetadata metadata,
            final @NotNull DateTime at
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.variants = variants;
        this.metadata = metadata;
        this.createdAt = at;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @Nullable String getTitle() {
        return this.title;
    }

    public @Nullable String getDescription() {
        return this.description;
    }

    public @NotNull Map<String, MediaVariant> getVariants() {
        return this.variants;
    }

    public @NotNull MediaMetadata getMetadata() {
        return this.metadata;
    }

    public @NotNull DateTime getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String toString() {
        return "Media{" + "id='" + this.id + '\'' + ", title='" + this.title + '\'' + ", description='" + this.description + '\'' + ", variants=" + this.variants + ", metadata=" + this.metadata + ", createdAt=" + this.createdAt + '}';
    }

}
