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
package gg.essential.mixins.transformers.client.gui;

import gg.essential.mixins.impl.util.math.Matrix4fExt;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

import static gg.essential.model.util.KotglKt.toMat4;

@Mixin(InventoryScreen.class)
public class Mixin_FixPlayerInventoryLightingOn116 {
    @Inject(method = "drawEntityOnScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderManager()Lnet/minecraft/client/renderer/entity/EntityRendererManager;"))
    private static void fixPlayerLighting(CallbackInfo ci) {
        Matrix4f matrix = new Matrix4f();
        // 1.16 is a weird mix of legacy gl stack and explicit pojo stack
        // The lighting setup method already ignores the gl stack but the renderer doesn't yet take the pojo stack
        // stack as a separate input, so one actually has to combine the two stacks when setting lighting for it
        // to function properly.
        // We can only pass a pojo stack to the method though, so we gotta convert the gl stack into one first.
        glGetModelViewMatrix(matrix);
        matrix.mul(Matrix4f.makeScale(1f, -1f, 1f));
        matrix.mul(Vector3f.YP.rotationDegrees(135f));
        RenderHelper.setupLevelDiffuseLighting(matrix);
    }

    @Unique
    private static void glGetModelViewMatrix(Matrix4f dst) {
        FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(16);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, buffer);
        float[] array = new float[16];
        buffer.get(array);
        ((Matrix4fExt) (Object) dst).setKotgl(toMat4(array));
    }

    @Inject(method = "drawEntityOnScreen", at = @At("TAIL"))
    private static void restoreDefaultGuiLighting(CallbackInfo ci) {
        RenderHelper.setupGui3DDiffuseLighting();
    }
}
