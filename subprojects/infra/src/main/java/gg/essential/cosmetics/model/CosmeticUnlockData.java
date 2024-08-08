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

import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CosmeticUnlockData {

    private final @NotNull @SerializedName("unlocked_at") DateTime unlockedAt;
    private final @Nullable @SerializedName("gifted_by") UUID giftedBy;
    private final @SerializedName("wardrobe_unlock") boolean wardrobeUnlock;

    public CosmeticUnlockData(final @NotNull DateTime unlockedAt, final @Nullable UUID giftedBy, final boolean wardrobeUnlock) {
        this.unlockedAt = unlockedAt;
        this.giftedBy = giftedBy;
        this.wardrobeUnlock = wardrobeUnlock;
    }

    public @Nullable UUID getGiftedBy() {
        return this.giftedBy;
    }

    public boolean isWardrobeUnlock() {
        return this.wardrobeUnlock;
    }

}
