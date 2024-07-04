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
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiNewChat.class)
public class Mixin_RemoveChatLimit {

    @ModifyExpressionValue(
        //#if MC>=12005
        //$$ method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
        //#elseif MC>=11901
        //$$ method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        //#else
        method = "setChatLine",
        //#endif
        at = @At(value = "CONSTANT", args = "intValue=100"))
    private int removeChatLimit(int original) {
        // we only want to adjust the value if it's still the default
        if (original == 100) {
            return 32767;
        }
        return original;
    }
}
