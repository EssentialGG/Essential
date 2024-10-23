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
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.BipedEntityRenderState;
//#endif

@Mixin(BipedArmorLayer.class)
public abstract class Mixin_StoreArmorRenderedState {

    private static final ThreadLocal<AbstractClientPlayerExt> playerThreadLocal = new ThreadLocal<>();

    //#if MC>=12102
    //$$ private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";
    //#else
    private static final String RENDER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#endif

    @Inject(method = RENDER, at = @At("HEAD"))
    private void essential$assumeArmorRenderingSuppressed(
        CallbackInfo ci,
        //#if MC>=12102
        //$$ @Local(argsOnly = true) BipedEntityRenderState state
        //#else
        @Local(argsOnly = true) LivingEntity entitylivingbaseIn
        //#endif
    ) {
        //#if MC>=12102
        //$$ if (!(state instanceof PlayerEntityRenderStateExt)) return;
        //$$ LivingEntity entitylivingbaseIn = ((PlayerEntityRenderStateExt) state).essential$getEntity();
        //#endif
        if (entitylivingbaseIn instanceof AbstractClientPlayerExt) {
            final AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entitylivingbaseIn;
            playerThreadLocal.set(playerExt);
            playerExt.assumeArmorRenderingSuppressed();
        }
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void resetThreadLocal(CallbackInfo ci) {
        playerThreadLocal.remove();
    }

    @Inject(method = "func_241739_a_", at = @At(value = "HEAD", shift = At.Shift.AFTER))
    private void essential$markRenderingNotSuppressed(
        CallbackInfo ci,
        @Local(argsOnly = true) EquipmentSlotType arg4
    ) {
        AbstractClientPlayerExt playerExt = playerThreadLocal.get();
        if (playerExt != null) {
            if (arg4.getSlotType() == EquipmentSlotType.Group.ARMOR) {
                playerExt.armorRenderingNotSuppressed(arg4.getIndex());
            }
        }
    }
}
