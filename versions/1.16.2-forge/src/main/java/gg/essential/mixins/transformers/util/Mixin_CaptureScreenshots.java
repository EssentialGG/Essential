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
import gg.essential.util.ExtensionsKt;
import gg.essential.util.Multithreading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ScreenShotHelper;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if FORGE
import net.minecraftforge.client.event.ScreenshotEvent;
//#endif

import java.io.File;
import java.util.function.Consumer;

@Mixin(ScreenShotHelper.class)
public class Mixin_CaptureScreenshots {

    private static final String WRITE_METHOD = "Lnet/minecraft/client/renderer/texture/NativeImage;write(Ljava/io/File;)V";

    @Unique
    private static void essential$captureNewScreenshot(File file) {
        ExtensionsKt.getExecutor(Minecraft.getInstance()).execute(() -> {
            final ScreenshotManager screenshotManager = Essential.getInstance().getConnectionManager().getScreenshotManager();

            ClientScreenshotMetadata metadata = screenshotManager.getCurrentMetadata();
            boolean essentialScreenshots = EssentialConfig.INSTANCE.getEssentialEnabled() && EssentialConfig.INSTANCE.getEssentialScreenshots();

            Multithreading.runAsync(() -> screenshotManager.handleNewScreenshot(file, ScreenshotComponentsKt.cloneWithNewChecksum(metadata, screenshotManager.getChecksum(file)), essentialScreenshots));
        });
    }

    //#if FORGE
    @Inject(method = {
        "lambda$saveScreenshotRaw$2", // early 1.16
        "lambda$_grab$2", // late 1.16 and above
    }, at = @At(value = "INVOKE", target = WRITE_METHOD, shift = At.Shift.AFTER))
    private static void essential$captureNewScreenshot(NativeImage nativeImage, File file, File fil1, ScreenshotEvent event, Consumer consumer, CallbackInfo ci) {
        //#else
        //$$ // remap disabled because we don't want the mixin AP to qualify the method reference, since optifine changes its arguments
        //$$ @Inject(method = "method_1661", at = @At(value = "INVOKE", target = WRITE_METHOD, shift = At.Shift.AFTER, remap = true), remap = false)
        //$$    private static void essential$captureNewScreenshot(NativeImage nativeImage, File file, Consumer consumer, CallbackInfo ci) {
        //#endif

        essential$captureNewScreenshot(file);
    }

    @Dynamic("Optifine changes arguments")
    @Surrogate
    private static void essential$captureNewScreenshot(NativeImage nativeImage, File file, Object event, Consumer consumer, CallbackInfo ci) {
        essential$captureNewScreenshot(file);
    }
}