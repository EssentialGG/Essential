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
package gg.essential.mixins.impl.client.entity;

import gg.essential.cosmetics.CosmeticsState;
import gg.essential.cosmetics.WearablesManager;
import gg.essential.cosmetics.source.CosmeticsSource;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.util.UIdentifier;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AbstractClientPlayerExt {
    CosmeticsSource getCosmeticsSource();

    void setCosmeticsSource(CosmeticsSource source);

    @NotNull WearablesManager getWearablesManager();

    @NotNull
    CosmeticsState getCosmeticsState();

    void setEssentialCosmeticsCape(@Nullable String cape, @Nullable List<UIdentifier> textures);

    ResourceLocation applyEssentialCosmeticsMask(ResourceLocation skin);

    boolean isSkinOverrodeByServer();

    void assumeArmorRenderingSuppressed();

    void armorRenderingNotSuppressed(int slot);

    boolean[] wasArmorRenderingSuppressed();

    @NotNull
    PlayerPoseManager getPoseManager();

    boolean isPoseModified();

    void setPoseModified(boolean poseModified);

    float getLastCosmeticsUpdateTime();

    void setLastCosmeticsUpdateTime(float time);
}
