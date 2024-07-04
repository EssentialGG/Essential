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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosmeticOutfit {

    @SerializedName("a")
    private final @NotNull String id;

    @SerializedName("b")
    private final @NotNull String name;

    @SerializedName("c")
    private @Nullable String skinTexture;

    @SerializedName("d")
    private @Nullable Map<@NotNull CosmeticSlot, @NotNull String> equippedCosmetics;

    @SerializedName("e")
    private @Nullable Map<@NotNull String, @NotNull List<@NotNull CosmeticSetting>> cosmeticSettings;

    @SerializedName("f")
    private boolean selected;

    @SerializedName("g")
    private final @NotNull DateTime createdAt;

    @SerializedName("h")
    private final @Nullable DateTime favoritedAt;

    // i: LastUsedAt (not used by mod)

    @SerializedName("j")
    private @Nullable String skinId;

    public CosmeticOutfit(
            final @NotNull String id,
            final @NotNull String name,
            final @Nullable String skinTexture,
            final @Nullable Map<@NotNull CosmeticSlot, @NotNull String> equippedCosmetics,
            final @Nullable Map<@NotNull String, @NotNull List<@NotNull CosmeticSetting>> cosmeticSettings,
            final boolean selected,
            final @NotNull DateTime createdAt,
            final @Nullable DateTime favoritedAt,
            final @Nullable String skinId
    ) {
        this.id = id;
        this.name = name;
        this.skinTexture = skinTexture;
        this.equippedCosmetics = equippedCosmetics;
        this.cosmeticSettings = cosmeticSettings;
        this.selected = selected;
        this.createdAt = createdAt;
        this.favoritedAt = favoritedAt;
        this.skinId = skinId;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull String getName() {
        return this.name;
    }

    public @Nullable String getSkinTexture() {
        return this.skinTexture;
    }

    public @NotNull Map<@NotNull CosmeticSlot, @NotNull String> getEquippedCosmetics() {
        if (this.equippedCosmetics == null) {
            this.equippedCosmetics = new HashMap<>();
        }

        return this.equippedCosmetics;
    }

    public @NotNull Map<@NotNull String, @NotNull List<@NotNull CosmeticSetting>> getCosmeticSettings() {
        if (this.cosmeticSettings == null) {
            this.cosmeticSettings = new HashMap<>();
        }

        return this.cosmeticSettings;
    }

    public @NotNull DateTime getCreatedAt() {
        return this.createdAt;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public @Nullable DateTime getFavoritedAt() {
        return this.favoritedAt;
    }

    public @Nullable String getSkinId() {
        return this.skinId;
    }

    public void setSkinTexture(final @Nullable String skinTexture) {
        this.skinTexture = skinTexture;
    }

    public void setSkinId(final @Nullable String skinId) {
        this.skinId = skinId;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }
}
