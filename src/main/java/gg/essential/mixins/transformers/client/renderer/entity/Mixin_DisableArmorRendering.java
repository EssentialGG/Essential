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
import gg.essential.mixins.impl.client.renderer.entity.ArmorRenderingUtil;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11200
import net.minecraft.inventory.EntityEquipmentSlot;
//#endif

@Mixin(value = LayerArmorBase.class)
public class Mixin_DisableArmorRendering {

    //#if MC>=11200
    @Inject(method = "renderArmorLayer", at = @At(value = "HEAD"), cancellable = true)
    private void essential$disableArmorRendering(CallbackInfo info, @Local(argsOnly = true) EntityLivingBase entityLivingBaseIn, @Local(argsOnly = true) EntityEquipmentSlot slotIn) {
        int slotIndex = slotIn.getIndex();
    //#else
    //$$ @Inject(method = "renderLayer", at = @At(value = "HEAD"), cancellable = true)
    //$$ private void essential$disableArmorRendering(EntityLivingBase entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, int slotIn, CallbackInfo info) {
    //$$     int slotIndex = slotIn-1;
    //#endif
        if (ArmorRenderingUtil.shouldDisableArmor(entityLivingBaseIn, slotIndex)) {
            info.cancel();
        }
    }
}
