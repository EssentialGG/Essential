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

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_RecalculateMenuScale {

    @Unique
    private boolean shouldRecomputeGuiScale = false;

    @Inject(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;onClose()V"))
    private void getOldScreen(CallbackInfo callbackInfo) {
        shouldRecomputeGuiScale = isMenu(Minecraft.getInstance().currentScreen);
    }

    @Inject(method = "displayGuiScreen", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/screen/Screen;",
            shift = At.Shift.AFTER,
            opcode = Opcodes.PUTFIELD
    ))
    private void recalculateScale(CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        MainWindow window = minecraft.getMainWindow();
        if (isMenu(minecraft.currentScreen) || shouldRecomputeGuiScale) {
            window.setGuiScale(window.calcGuiScale(
                    //#if MC>=11900
                    //$$ minecraft.options.getGuiScale().getValue(), minecraft.options.getForceUnicodeFont().getValue()
                    //#else
                    minecraft.gameSettings.guiScale, minecraft.gameSettings.forceUnicodeFont
                    //#endif
            ));
        }
    }

    private boolean isMenu(Screen screen) {
        return screen instanceof MainMenuScreen || screen instanceof IngameMenuScreen;
    }

}
