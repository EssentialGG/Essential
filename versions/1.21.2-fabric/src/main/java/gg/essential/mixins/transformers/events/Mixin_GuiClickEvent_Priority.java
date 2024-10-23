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
// 1.21.2 and above
package gg.essential.mixins.transformers.events;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.Essential;
import gg.essential.event.gui.GuiClickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class, priority = 500)
public abstract class Mixin_GuiClickEvent_Priority  {
    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"), cancellable = true)
    private void onMouseClicked(
        CallbackInfo ci,
        @Local(ordinal = 0) Screen screen,
        @Local(ordinal = 0, argsOnly = true) int mouseButton,
        @Local(ordinal = 0) double mouseX,
        @Local(ordinal = 1) double mouseY
    ) {
        GuiClickEvent event = new GuiClickEvent.Priority(mouseX, mouseY, mouseButton, screen);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled() || MinecraftClient.getInstance().currentScreen != screen) {
            ci.cancel();
        }
    }
}
