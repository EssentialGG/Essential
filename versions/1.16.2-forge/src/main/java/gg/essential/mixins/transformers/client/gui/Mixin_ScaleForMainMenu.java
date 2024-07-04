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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.handlers.PauseMenuDisplay;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MainWindow.class)
public class Mixin_ScaleForMainMenu {
    @ModifyExpressionValue(method = "calcGuiScale", at = @At(value = "CONSTANT", args = "intValue=320"))
    private int modifyMinWidth(int original) {
        Screen screen = UMinecraft.getMinecraft().currentScreen;
        return screen != null && PauseMenuDisplay.canRescale(screen) ? PauseMenuDisplay.getMinWidth() : original;
    }
}
