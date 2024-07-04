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

import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import net.minecraft.client.KeyboardListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

@Mixin(KeyboardListener.class)
public class Mixin_CancelVanillaScreenshotHotkey {

    // We want to override the Minecraft keybind from activating because we may need to wait
    // one frame so the game can render again without notifications or other screenshots

    @Redirect(method = "onKeyEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;Ljava/util/function/Consumer;)V"))
    private void essential$screenshotTake(File gameDirectory, int width, int height, Framebuffer buffer, Consumer<ITextComponent> messageConsumer) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            Essential.getInstance().getConnectionManager().getScreenshotManager().handleScreenshotKeyPressed();
        } else {
            ScreenShotHelper.saveScreenshot(gameDirectory, width, height, buffer, messageConsumer);
        }
    }
}
