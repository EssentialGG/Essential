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
import gg.essential.cosmetics.CosmeticSlot;
import gg.essential.holder.DisplayNameHolder;
import gg.essential.model.EssentialAsset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class CosmeticCategory implements DisplayNameHolder {

    @SerializedName("a")
    private final @NotNull String id;

    @SerializedName("b")
    private final @NotNull Map<@NotNull String, @NotNull String> displayNames;

    @SerializedName("c")
    private final @NotNull EssentialAsset icon;

    @SerializedName("d")
    private final @Nullable Set<@NotNull CosmeticSlot> slots;

    @SerializedName("e")
    private final @Nullable Set<@NotNull String> tags;

    @SerializedName("f")
    private final int order;

    @SerializedName("g")
    private final @Nullable DateTime availableAfter;

    @SerializedName("h")
    private final @Nullable DateTime availableUntil;

    @SerializedName("i")
    private @Nullable Map<@NotNull String, @NotNull String> compactNames;

    @SerializedName("j")
    private @Nullable Map<@NotNull String, @NotNull String> descriptions;

    public CosmeticCategory(
            final @NotNull String id,
            final @NotNull Map<@NotNull String, @NotNull String> displayNames,
            final @NotNull EssentialAsset icon,
            final @Nullable Set<@NotNull CosmeticSlot> slots,
            final @Nullable Set<String> tags,
            final int order,
            final @Nullable DateTime availableAfter,
            final @Nullable DateTime availableUntil,
            final @Nullable Map<@NotNull String, String> compactNames,
            final @Nullable Map<@NotNull String, String> descriptions
    ) {
        this.id = id;
        this.displayNames = displayNames;
        this.icon = icon;
        this.slots = slots;
        this.tags = tags;
        this.order = order;
        this.availableAfter = availableAfter;
        this.availableUntil = availableUntil;
        this.compactNames = compactNames;
        this.descriptions = descriptions;
    }


    public @NotNull String getId() {
        return this.id;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getDisplayNames() {
        return this.displayNames;
    }

    public @NotNull EssentialAsset getIcon() {
        return this.icon;
    }

    public @Nullable Set<@NotNull CosmeticSlot> getSlots() {
        return this.slots;
    }

    public @Nullable Set<@NotNull String> getTags() {
        return this.tags;
    }

    public int getOrder() {
        return this.order;
    }

    public @Nullable DateTime getAvailableAfter() {
        return this.availableAfter;
    }

    public @Nullable DateTime getAvailableUntil() {
        return this.availableUntil;
    }

    public @NotNull Map<@NotNull String, @NotNull String> getCompactNames() {
        if (this.compactNames == null) {
            this.compactNames = Collections.emptyMap();
        }

        return this.compactNames;
    }

    public @Nullable String getCompactName(final @NotNull String key) {
        return this.getCompactNames().get(key);
    }

    public @NotNull Map<@NotNull String, @NotNull String> getDescriptions() {
        if (this.descriptions == null) {
            this.descriptions = Collections.emptyMap();
        }

        return this.descriptions;
    }

    public @Nullable String getDescription(final @NotNull String key) {
        return this.getDescriptions().get(key);
    }

}
