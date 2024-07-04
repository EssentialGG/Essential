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
import gg.essential.cosmetics.EquippedCosmetic;
import gg.essential.mod.cosmetics.CosmeticSlot;
import org.jetbrains.annotations.NotNull;

public class ConfigurableCosmeticsSource implements CosmeticsSource {

    private ImmutableMap<CosmeticSlot, EquippedCosmetic> cosmetics = ImmutableMap.of();

    private boolean shouldOverrideRenderCosmeticsCheck = false;

    public void setCosmetics(ImmutableMap<CosmeticSlot, EquippedCosmetic> cosmetics) {
        this.cosmetics = cosmetics;
    }

    public void setShouldOverrideRenderCosmeticsCheck(boolean shouldOverrideRenderCosmeticsCheck) {
        this.shouldOverrideRenderCosmeticsCheck = shouldOverrideRenderCosmeticsCheck;
    }

    @Override
    @NotNull
    public ImmutableMap<CosmeticSlot, EquippedCosmetic> getCosmetics() {
        return this.cosmetics;
    }

    @Override
    public boolean getShouldOverrideRenderCosmeticsCheck() {
        return this.shouldOverrideRenderCosmeticsCheck;
    }
}
