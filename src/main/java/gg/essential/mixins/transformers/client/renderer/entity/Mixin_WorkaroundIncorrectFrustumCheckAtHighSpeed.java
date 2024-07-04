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

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla frustum checks entities by using their bounding box with no consideration for their velocity (i.e. not taking
 * partialTicks into account). Therefore, if you give yourself a high level of speed, switch to third person and start
 * running towards the camera, your player will flicker in and out of existence depending on whether its bounding box is
 * currently in sight or already ahead of the camera. And because we've been blamed for it even though it's a vanilla
 * thing (presumably because people started playing in third person more to see their cosmetics), we'll go ahead and try
 * to fix it (or at least reduce the chance of it happening).
 *
 * To work around that in a minimally invasive way, we skip the frustum check for the client player. That won't fix it
 * if someone is running right next to you but that seems unlikely and fixing it in the general case
 * We do this here because we can use a simple inject, rather than having to override the method if we were to do it in
 * the player renderer.
 */
@Mixin(Render.class)
public abstract class Mixin_WorkaroundIncorrectFrustumCheckAtHighSpeed<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void workaroundFastPlayerFrustumCheck(T entity, ICamera camera, double x, double y, double z, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof EntityPlayerSP) {
            ci.setReturnValue(true);
        }
    }

}
