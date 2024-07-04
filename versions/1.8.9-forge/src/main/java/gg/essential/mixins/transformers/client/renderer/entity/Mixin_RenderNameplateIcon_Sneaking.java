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
import gg.essential.handlers.OnlineIndicator;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// On 1.8.9, sneaking entity's nametags are rendered using a copy-paste of the nametag rendering code in another class :)
@Mixin(RendererLivingEntity.class)
public class Mixin_RenderNameplateIcon_Sneaking<T extends EntityLivingBase> {
    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V", shift = At.Shift.AFTER))
    private void essential$translateNameplate(CallbackInfo ci, @Local(argsOnly = true) T entity) {
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableLighting()V"))
    private void renderEssentialIndicator(T entityIn, double x, double y, double z, CallbackInfo ci, @Local String str) {
        if (entityIn instanceof EntityPlayer) {
            int light = (((int) OpenGlHelper.lastBrightnessY) << 16) + (int) OpenGlHelper.lastBrightnessX;
            OnlineIndicator.drawNametagIndicator(new UMatrixStack(), entityIn, str, light);
        }
    }
}
