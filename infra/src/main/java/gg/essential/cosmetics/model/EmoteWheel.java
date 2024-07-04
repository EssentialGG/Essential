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
import com.sparkuniverse.toolbox.util.DateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EmoteWheel {

    @SerializedName("a")
    private final String id;

    @SerializedName("b")
    private final boolean selected;

    @SerializedName("c")
    private final Map<Integer, String> slots;

    @SerializedName("d")
    private final DateTime createdAt;

    @SerializedName("e")
    private @Nullable final DateTime updatedAt;

    public EmoteWheel(
            final @NotNull String id,
            final boolean selected,
            final @NotNull Map<Integer, String> slots,
            final @NotNull DateTime createdAt,
            final @Nullable DateTime updatedAt
    ) {
        this.id = id;
        this.selected = selected;
        this.slots = slots;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String id() {
        return id;
    }

    public boolean selected() {
        return selected;
    }

    public Map<Integer, String> slots() {
        return slots;
    }

    public DateTime createdAt() {
        return createdAt;
    }

    public @Nullable DateTime updatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "EmoteWheel{" +
                "id='" + id + '\'' +
                ", selected=" + selected +
                ", slots=" + slots +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
