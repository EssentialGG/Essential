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

import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC==11202
import net.minecraft.inventory.EntityEquipmentSlot;
//#endif

@Mixin(value = LayerArmorBase.class, priority = 900)
public class Mixin_StoreArmorRenderedState {

    @Inject(method = "doRenderLayer", at = @At("HEAD"))
    private void essential$assumeArmorRenderingSuppressed(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo info) {
        if (entitylivingbaseIn instanceof AbstractClientPlayerExt) {
            final AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entitylivingbaseIn;
            playerExt.assumeArmorRenderingSuppressed();
        }
    }

    //#if MC==11202
    @Inject(method = "renderArmorLayer", at = @At(value = "HEAD", shift = At.Shift.AFTER))
    private void essential$markRenderingNotSuppressed(EntityLivingBase entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slotIn, CallbackInfo info) {
        int slotIndex = slotIn.getIndex();
    //#else if MC==10809
    //$$ @Inject(method = "renderLayer", at = @At(value = "HEAD", shift = At.Shift.AFTER))
    //$$ private void essential$markRenderingNotSuppressed(EntityLivingBase entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, int slotIn, CallbackInfo info) {
    //$$     int slotIndex = slotIn-1;
    //#endif
        if (entityLivingBaseIn instanceof AbstractClientPlayerExt) {
            final AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entityLivingBaseIn;
            playerExt.armorRenderingNotSuppressed(slotIndex);
        }
    }
}
