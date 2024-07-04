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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.Essential;
import gg.essential.universal.UKeyboard;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class Mixin_ChatPeekScrolling {

    @ModifyExpressionValue(method =
        //#if MC>=11202
        "runTickMouse",
        //#else
        //$$ "runTick",
        //#endif
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int essential$getScrollAmount(int direction) {
        if (Essential.getInstance().getKeybindingRegistry().isHoldingChatPeek() && direction != 0) {
            int scrollAmount = direction > 0 ? 1 : -1;
            // Copy Minecraft scroll logic from GuiChat#handleMouseInput
            if (!UKeyboard.isShiftKeyDown()) {
                scrollAmount *= 7;
            }
            Minecraft.getMinecraft().ingameGUI.getChatGUI().scroll(scrollAmount);
            return 0; // Cancel scroll event
        }
        return direction;
    }

}
