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

import com.mojang.blaze3d.matrix.MatrixStack;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedArmorLayer.class)
public abstract class Mixin_StoreArmorRenderedState<T extends LivingEntity, A extends BipedModel<T>> {

    @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void essential$assumeArmorRenderingSuppressed(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
        if (entitylivingbaseIn instanceof AbstractClientPlayerExt) {
            final AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entitylivingbaseIn;
            playerExt.assumeArmorRenderingSuppressed();
        }
    }

    @Inject(method = "func_241739_a_", at = @At(value = "HEAD", shift = At.Shift.AFTER))
    private void essential$markRenderingNotSuppressed(MatrixStack arg, IRenderTypeBuffer arg2, T arg3, EquipmentSlotType arg4, int j, A arg5, CallbackInfo info) {
        if (arg3 instanceof AbstractClientPlayerExt) {
            final AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) arg3;
            if (arg4.getSlotType() == EquipmentSlotType.Group.ARMOR) {
                playerExt.armorRenderingNotSuppressed(arg4.getIndex());
            }
        }
    }
}
