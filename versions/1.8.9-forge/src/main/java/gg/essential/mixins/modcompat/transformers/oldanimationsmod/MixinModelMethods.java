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
package gg.essential.mixins.modcompat.transformers.oldanimationsmod;

import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.ModelBipedUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fixes OAM conflicting with emotes
@Pseudo
@Mixin(targets = {"com.orangemarshall.animations.util.ModelMethods"})
public class MixinModelMethods {
    @Inject(method = "setRotationAnglesModelBiped", at = @At("HEAD"), remap = false)
    private static void resetPose(
        ModelBiped model,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scaleFactor,
        Entity entity,
        CallbackInfo ci
    ) {
        ModelBipedUtil.resetPose(model);
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Inject(method = "setRotationAnglesModelBiped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;copyModelAngles(Lnet/minecraft/client/model/ModelRenderer;Lnet/minecraft/client/model/ModelRenderer;)V", remap = true), remap = false)
    private static void applyPoseTransform(
        ModelBiped model,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scaleFactor,
        Entity entity,
        CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
        ModelBipedUtil.applyPoseTransform(model, cState);
    }
}
