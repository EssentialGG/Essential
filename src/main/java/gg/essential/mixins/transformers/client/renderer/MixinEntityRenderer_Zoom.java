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
package gg.essential.mixins.transformers.client.renderer;

import gg.essential.handlers.ZoomHandler;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer_Zoom {
    //#if MC>=11600
    //$$ @ModifyVariable(method = "getFOVModifier", at = @At(value = "STORE", ordinal = 1), ordinal = 0)
    //$$ private double applyZoomModifiers(double f) {
    //#else
    @ModifyVariable(method = "getFOVModifier", at = @At(value = "STORE", ordinal = 1), ordinal = 1)
    private float applyZoomModifiers(float f) {
    //#endif
        //noinspection RedundantCast
        return ZoomHandler.getInstance().applyModifiers((float) f);
    }
}
