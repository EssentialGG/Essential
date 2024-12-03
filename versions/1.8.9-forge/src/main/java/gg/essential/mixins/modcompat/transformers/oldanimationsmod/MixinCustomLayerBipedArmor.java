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
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fixes OAM conflicting with the 'Show my armor' option is set to 'Hide conflicting armor'
// This only occurs when the third person block position option is enabled.
@Pseudo
@Mixin(targets = {"com.orangemarshall.animations.util.CustomLayerBipedArmor"})
public class MixinCustomLayerBipedArmor {
    @Inject(method = "renderLayer", at = @At(value = "HEAD"), cancellable = true)
    private void essential$disableArmorRendering(
        EntityLivingBase entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        int slotIn,
        CallbackInfo info
    ) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        CosmeticsRenderState cState = new CosmeticsRenderState.Live((AbstractClientPlayer) entity);
        int slotIndex = slotIn - 1;
        if (cState.blockedArmorSlots().contains(slotIndex)) {
            info.cancel();
        }
    }
}
