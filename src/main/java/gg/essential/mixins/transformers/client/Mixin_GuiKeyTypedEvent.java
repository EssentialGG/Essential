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

import gg.essential.Essential;
import gg.essential.event.gui.GuiKeyTypedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class Mixin_GuiKeyTypedEvent {
    @Shadow public Minecraft mc;

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(CallbackInfo ci) {
        char typedChar = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();
        if ((keyCode != 0 || typedChar < ' ') && !Keyboard.getEventKeyState()) {
            return;
        }

        // Bypass our event for special keys handled by `Minecraft.dispatchKeypresses`
        Minecraft mc = this.mc;
        int keyBindCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        //#if MC>=11200
        if (mc.gameSettings.keyBindFullscreen.isActiveAndMatches(keyBindCode)) {
            return;
        } else if (mc.gameSettings.keyBindScreenshot.isActiveAndMatches(keyBindCode)) {
            return;
        } else if (keyBindCode == Keyboard.KEY_B && GuiScreen.isCtrlKeyDown() && (mc.currentScreen == null || !mc.currentScreen.isFocused())) {
            return;
        }
        //#else
        //$$ if (keyBindCode == mc.gameSettings.keyBindStreamStartStop.getKeyCode()) {
        //$$     return;
        //$$ } else if (keyBindCode == mc.gameSettings.keyBindStreamPauseUnpause.getKeyCode()) {
        //$$     return;
        //$$ } else if (keyBindCode == mc.gameSettings.keyBindStreamCommercials.getKeyCode()) {
        //$$     return;
        //$$ } else if (keyBindCode == mc.gameSettings.keyBindStreamToggleMic.getKeyCode()) {
        //$$     return;
        //$$ } else if (keyBindCode == mc.gameSettings.keyBindFullscreen.getKeyCode()) {
        //$$     return;
        //$$ } else if (keyBindCode == mc.gameSettings.keyBindScreenshot.getKeyCode()) {
        //$$     return;
        //$$ }
        //#endif

        GuiKeyTypedEvent event = new GuiKeyTypedEvent((GuiScreen) (Object) this, typedChar, keyCode);
        Essential.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
