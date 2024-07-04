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

import gg.essential.mixins.impl.client.model.ModelBipedExt;
import gg.essential.mixins.impl.client.model.ModelBipedUtil;
import gg.essential.model.backend.PlayerPose;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public abstract class Mixin_ApplyPoseTransform implements ModelBipedExt {

    //#if MC>=11600
    //$$ private static final String COPY_MODEL_ANGLES = "Lnet/minecraft/client/renderer/model/ModelRenderer;copyModelAngles(Lnet/minecraft/client/renderer/model/ModelRenderer;)V";
    //#else
    private static final String COPY_MODEL_ANGLES = "Lnet/minecraft/client/model/ModelBiped;copyModelAngles(Lnet/minecraft/client/model/ModelRenderer;Lnet/minecraft/client/model/ModelRenderer;)V";
    //#endif

    @Unique
    private PlayerPose resetPose;

    @Override
    public PlayerPose getResetPose() {
        return resetPose;
    }

    @Override
    public void setResetPose(PlayerPose pose) {
        resetPose = pose;
    }

    /**
     * Reset the vanilla models back to their initial pose. This is necessary because the vanilla code is not guaranteed
     * to reset all values by itself (only the ones it also modifies itself).
     */
    @Inject(method = "setRotationAngles", at = @At("HEAD"))
    private void resetPose(CallbackInfo ci) {
        ModelBipedUtil.resetPose((ModelBiped) (Object) this);
    }

    @Inject(method = "setRotationAngles", at = @At(value = "INVOKE", target = COPY_MODEL_ANGLES))
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
        ModelBipedUtil.applyPoseTransform((ModelBiped) (Object) this, entity);
    }
}
