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
import gg.essential.config.EssentialConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(Minecraft.class)
public class Mixin_CancelVanillaScreenshotHotkey {

    // We want to override the Minecraft keybind from activating because we may need to wait
    // one frame so the game can render again without notifications or other screenshots

    @Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;"))
    private ITextComponent run(File file, int width, int height, Framebuffer framebuffer) {
        if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
            Essential.getInstance().getConnectionManager().getScreenshotManager().handleScreenshotKeyPressed();
            return null;
        } {
            return ScreenShotHelper.saveScreenshot(file, width, height, framebuffer);
        }
    }

    @WrapWithCondition(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;printChatMessage(Lnet/minecraft/util/text/ITextComponent;)V"))
    private boolean print(GuiNewChat chat, ITextComponent component) {
        // The above mixin will return null for this arg and want to consume that
        return component != null;
    }
}
