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
package gg.essential.mixins.transformers.feature.chat;


import gg.essential.Essential;
import gg.essential.universal.UKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class Mixin_ChatPeekScrolling {

    @Inject(method = "scrollCallback", at = @At("HEAD"), cancellable = true)
    private void essential$chatPeek(long window, double xoffset, double yoffset, CallbackInfo ci) {

        if (window != Minecraft.getInstance().getMainWindow().getHandle()) {
            return;
        }

        if (Essential.getInstance().getKeybindingRegistry().isHoldingChatPeek() && yoffset != 0) {
            int scrollAmount = yoffset > 0 ? 1 : -1;
            // Copy Minecraft scroll logic from GuiChat#handleMouseInput
            if (!UKeyboard.isShiftKeyDown()) {
                scrollAmount *= 7;
            }
            Minecraft.getInstance().ingameGUI.getChatGUI().addScrollPos(scrollAmount);
            ci.cancel();
        }
    }
}
