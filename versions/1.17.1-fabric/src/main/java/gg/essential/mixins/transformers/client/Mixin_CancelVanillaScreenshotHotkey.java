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
import net.minecraft.client.Keyboard;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Keyboard.class)
public class Mixin_CancelVanillaScreenshotHotkey {

    // We want to override the Minecraft keybind from activating because we may need to wait
    // one frame so the game can render again without notifications or other screenshots

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/ScreenshotRecorder;saveScreenshot(Ljava/io/File;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V"))
    private void essential$screenshotTake(File gameDirectory, Framebuffer buffer, Consumer<Text> messageConsumer) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            Essential.getInstance().getConnectionManager().getScreenshotManager().handleScreenshotKeyPressed();
        } else {
            ScreenshotRecorder.saveScreenshot(gameDirectory, buffer, messageConsumer);
        }
    }
}
