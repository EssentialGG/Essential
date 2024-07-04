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
package gg.essential.cosmetics.source;

import com.google.common.collect.ImmutableMap;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.cosmetics.EquippedCosmetic;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LiveCosmeticsSource implements CosmeticsSource {

    private final CosmeticsManager cosmeticsManager;
    private final UUID uuid;

    public LiveCosmeticsSource(CosmeticsManager cosmeticsManager, UUID uuid) {
        this.cosmeticsManager = cosmeticsManager;
        this.uuid = uuid;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @Override
    @NotNull
    public ImmutableMap<CosmeticSlot, EquippedCosmetic> getCosmetics() {
        return this.cosmeticsManager.getVisibleCosmetics(this.uuid);
    }

    @Override
    public boolean getShouldOverrideRenderCosmeticsCheck() {
        return false;
    }
}
