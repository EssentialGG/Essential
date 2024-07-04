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
package gg.essential.mixins.transformers.client.renderer.entity;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.config.EssentialConfig;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.mixins.impl.client.model.ElytraPoseSupplier;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.model.backend.minecraft.PlayerPoseKt.toPose;

@Mixin(ModelElytra.class)
public abstract class Mixin_ApplyPoseTransform_Elytra implements ElytraPoseSupplier {

    @Shadow
    @Final
    private ModelRenderer leftWing;

    @Shadow
    @Final
    private ModelRenderer rightWing;

    @Unique
    private PlayerPose resetPose;

    /**
     * Reset the vanilla models back to their initial pose. This is necessary because the vanilla code is not guaranteed
     * to reset all values by itself (only the ones it also modifies itself).
     */
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void resetPose(
        CallbackInfo ci,
        //#if MC>=11400
        //$$ @Local(argsOnly = true) net.minecraft.entity.LivingEntity entity
        //#else
        @Local(argsOnly = true) Entity entity
        //#endif
    ) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        ModelElytra model = (ModelElytra) (Object) this;

        if (resetPose == null) {
            resetPose = PlayerPoseKt.withElytraPose(PlayerPose.Companion.neutral(), this.leftWing, this.rightWing, player);
        } else {
            boolean isChild = model.isChild; // child isn't set by the method we inject into, so we need to preserve it
            PlayerPoseKt.applyElytraPose(resetPose, this.leftWing, this.rightWing, player);
            model.isChild = isChild;
        }
    }

    @Inject(method = "setRotationAngles", at = @At("TAIL"))
    private void applyPoseTransform(
        //#if MC>=11400
        //$$ net.minecraft.entity.LivingEntity entity,
        //#endif
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        //#if MC<11400
        float scaleFactor,
        Entity entity,
        //#endif
        CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayerExt)) return;
        if (EssentialConfig.INSTANCE.getDisableEmotes() && !(entity instanceof EmulatedUI3DPlayer.EmulatedPlayer)) {
            return;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entity;

        PlayerPose basePose = PlayerPoseKt.withElytraPose(PlayerPose.Companion.neutral(), this.leftWing, this.rightWing, player);
        PlayerPose transformedPose = playerExt.getPoseManager().computePose(playerExt.getWearablesManager(), basePose);

        if (basePose.equals(transformedPose)) {
            return;
        }

        PlayerPoseKt.applyElytraPose(transformedPose, this.leftWing, this.rightWing, player);
    }

    @Override
    public @Nullable PlayerPose.Part getLeftWingPose() {
        return toPose(this.leftWing);
    }

    @Override
    public @Nullable PlayerPose.Part getRightWingPose() {
        return toPose(this.rightWing);
    }
}
