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

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.Essential;
import gg.essential.event.gui.GuiKeyTypedEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Keyboard.class, priority = 500)
public class Mixin_GuiKeyTypedEvent {
    @Unique
    private static void keyTyped(Screen screen, char typedChar, int keyCode, CallbackInfo ci) {
        GuiKeyTypedEvent event = new GuiKeyTypedEvent(screen, typedChar, keyCode);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;keyPressed(III)Z"), cancellable = true)
    private void onKeyTyped(CallbackInfo ci, @Local Screen screen, @Local(ordinal = 0, argsOnly = true) int key) {
        keyTyped(screen, '\0', key, ci);
    }

    @Inject(method = "onChar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;charTyped(CI)Z"), cancellable = true)
    private void onCharTyped(
        CallbackInfo ci,
        @Local Screen screen,
        @Local(ordinal = 0, argsOnly = true) int codePoint
    ) {
        if (Character.isBmpCodePoint(codePoint)) {
            keyTyped(screen, (char) codePoint, 0, ci);
        } else if (Character.isValidCodePoint(codePoint)) {
            keyTyped(screen, Character.highSurrogate(codePoint), 0, ci);
            keyTyped(screen, Character.lowSurrogate(codePoint), 0, ci);
        }
    }
}
