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

import java.util.UUID;

public class MediaMetadata {

    @SerializedName("a")
    private final @NotNull UUID authorId;

    @SerializedName("b")
    private final @NotNull DateTime time;

    @SerializedName("d")
    private final @NotNull MediaLocationMetadata locationMetadata;

    @SerializedName("e")
    private boolean favorite;

    @SerializedName("f")
    private boolean edited;

    public MediaMetadata(
            final @NotNull UUID authorId,
            final @NotNull DateTime time,
            final @NotNull MediaLocationMetadata locationMetadata,
            final boolean favorite,
            final boolean edited
    ) {
        this.authorId = authorId;
        this.time = time;
        this.locationMetadata = locationMetadata;
        this.favorite = favorite;
        this.edited = edited;
    }

    public @NotNull UUID getAuthorId() {
        return this.authorId;
    }

    public @NotNull DateTime getTime() {
        return this.time;
    }

    public @NotNull MediaLocationMetadata getLocationMetadata() {
        return this.locationMetadata;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public boolean isEdited() {
        return this.edited;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    public void setEdited(final boolean edited) {
        this.edited = edited;
    }

    @Override
    public @NotNull String toString() {
        return "MediaMetadata{" +
                "authorId=" + this.authorId +
                ", time=" + this.time +
                ", locationMetadata=" + this.locationMetadata +
                ", favorite=" + this.favorite +
                ", edited=" + this.edited +
                '}';
    }

}
