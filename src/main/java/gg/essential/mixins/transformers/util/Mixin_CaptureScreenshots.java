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
package gg.essential.mixins.transformers.util;


import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import gg.essential.gui.screenshot.components.ScreenshotComponentsKt;
import gg.essential.handlers.screenshot.ClientScreenshotMetadata;
import gg.essential.network.connectionmanager.media.ScreenshotManager;
import gg.essential.util.Multithreading;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

@Mixin(ScreenShotHelper.class)
public abstract class Mixin_CaptureScreenshots {

    @Unique
    private static RenderedImage latestImage = null;

    @Unique
    private static File latestFile = null;

    @ModifyArg(method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;",
        at = @At(value = "INVOKE", target = "Ljavax/imageio/ImageIO;write(Ljava/awt/image/RenderedImage;Ljava/lang/String;Ljava/io/File;)Z"))
    private static File essential$captureVariables(RenderedImage image, String format, File output) {
        latestImage = image;
        latestFile = output;
        return output;
    }

    // Temporarily replaced with redirect until WrapWithCondition is updated to support wrapping non-void returning methods that drop the return value
    @Redirect(method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;",
        at = @At(value = "INVOKE", target = "Ljavax/imageio/ImageIO;write(Ljava/awt/image/RenderedImage;Ljava/lang/String;Ljava/io/File;)Z"))
    private static boolean essential$handleAsyncScreenshots(RenderedImage image, String format, File file) throws IOException {
        if (callVanillaImageWrite(image, file)) {
            ImageIO.write(image, format, file);
        }
        return true;
    }

    @Unique
    private static boolean callVanillaImageWrite(RenderedImage image, File file) throws IOException {
        final ScreenshotManager screenshotManager = Essential.getInstance().getConnectionManager().getScreenshotManager();
        final ClientScreenshotMetadata currentMetadata = screenshotManager.getCurrentMetadata();

        if (essential$isScreenshotsEnabled()) {
            // This is required to be done before we fork because if another screenshot is taken
            // in the same second, Minecraft will assign the same file name as it think that name is available.
            file.createNewFile();
            screenshotManager.saveScreenshotAsync(image, file, currentMetadata);
            latestFile = null; // Otherwise essential$captureNewScreenshots will trigger
            return false;
        }
        return true;
    }

    @Inject(method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;",
        at = @At(value = "INVOKE", target = "Ljavax/imageio/ImageIO;write(Ljava/awt/image/RenderedImage;Ljava/lang/String;Ljava/io/File;)Z", shift = At.Shift.AFTER))
    private static void essential$captureNewScreenshots(File gameDirectory, @Nullable String screenshotName, int width, int height, Framebuffer buffer, CallbackInfoReturnable<ITextComponent> info) {
        if (essential$isScreenshotsEnabled() || latestFile == null) {
            return;
        }
        final ScreenshotManager screenshotManager = Essential.getInstance().getConnectionManager().getScreenshotManager();
        final ClientScreenshotMetadata currentMetadata = screenshotManager.getCurrentMetadata();


        Multithreading.runAsync(() -> {
            final String checksum = screenshotManager.getChecksum(latestFile);
            if (checksum == null) {
                Essential.logger.info("Unable to read checksum for screenshot " + latestFile.getAbsolutePath());
                return;
            }
            screenshotManager.handleNewScreenshot(
                latestFile,
                ScreenshotComponentsKt.cloneWithNewChecksum(currentMetadata, checksum),
                false
            );
        });
    }

    @Unique
    private static boolean essential$isScreenshotsEnabled() {
        return EssentialConfig.INSTANCE.getEssentialScreenshots();
    }

}
