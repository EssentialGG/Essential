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
package gg.essential.mixins.transformers.events;

import gg.essential.Essential;
import gg.essential.event.gui.MouseScrollEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class Mixin_MouseScrollEvent {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "scrollCallback", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        Screen screen = this.minecraft.currentScreen;
        //#if MC>=11900
        //$$ boolean discreteMouseScroll = this.client.options.getDiscreteMouseScroll().getValue();
        //$$ double mouseWheelSensitivity = this.client.options.getMouseWheelSensitivity().getValue();
        //#else
        boolean discreteMouseScroll = this.minecraft.gameSettings.discreteMouseScroll;
        double mouseWheelSensitivity = this.minecraft.gameSettings.mouseWheelSensitivity;
        //#endif
        double scrollDelta = (discreteMouseScroll ? Math.signum(yoffset) : yoffset) * mouseWheelSensitivity;
        MouseScrollEvent event = new MouseScrollEvent(scrollDelta, screen);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }

        if (this.minecraft.currentScreen != screen) {
            ci.cancel(); // screen was closed, prevent the event from reaching the new screen
        }
    }
}
