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
package gg.essential.connectionmanager.common.packet.media;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.media.model.MediaMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientMediaCreatePacket extends Packet {

    @SerializedName("a")
    private final @NotNull String mediaId;

    @SerializedName("b")
    private final @Nullable String title;

    @SerializedName("c")
    private final @Nullable String description;

    @SerializedName("d")
    private final @NotNull MediaMetadata metadata;

    public ClientMediaCreatePacket(
            final @NotNull String mediaId,
            final @Nullable String title,
            final @Nullable String description,
            final @NotNull MediaMetadata metadata
    ) {
        this.mediaId = mediaId;
        this.title = title;
        this.description = description;
        this.metadata = metadata;
    }

    public @NotNull String getMediaId() {
        return this.mediaId;
    }

    public @Nullable String getTitle() {
        return this.title;
    }

    public @Nullable String getDescription() {
        return this.description;
    }

    public @NotNull MediaMetadata getMetadata() {
        return this.metadata;
    }

}
