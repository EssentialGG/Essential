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
package gg.essential.mixins.transformers.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.essential.Essential;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class Mixin_SuppressScreenshotBufferFlip {

    // If there are any overlay items rendered on screen, the ScreenshotManager will suppress those for the next call
    // and then capture the next frame. To avoid these on-screen elements from flickering, the buffer swap is suppressed.

    @WrapWithCondition(method = "updateDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V"))
    private boolean essential$swapBuffers() {
        return !Essential.getInstance().getConnectionManager().getScreenshotManager().suppressBufferSwap();
    }
}
