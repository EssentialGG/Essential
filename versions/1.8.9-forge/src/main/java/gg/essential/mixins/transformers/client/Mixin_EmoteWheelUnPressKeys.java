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

import gg.essential.gui.emotes.EmoteWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On 1.12.2 and above, {@code KeyBinding.unPressAllKeys()} is called in displayGuiScreen.
 * On 1.8.9, we need to implement this behavior when entering a gui from the emote wheel to prevent issues such as inventory walking.
 */
@Mixin(Minecraft.class)
public class Mixin_EmoteWheelUnPressKeys {

    @Shadow private GuiScreen currentScreen;

    private GuiScreen essential$previousScreen;

    @Inject(
        method = "displayGuiScreen",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", opcode = Opcodes.PUTFIELD)
    )
    private void essential$storePreviousScreen(CallbackInfo ci) {
        essential$previousScreen = this.currentScreen;
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setIngameNotInFocus()V"))
    private void essential$unPressKeys(CallbackInfo ci) {
        if (essential$previousScreen instanceof EmoteWheel) {
            KeyBinding.unPressAllKeys();
        }
    }
}
