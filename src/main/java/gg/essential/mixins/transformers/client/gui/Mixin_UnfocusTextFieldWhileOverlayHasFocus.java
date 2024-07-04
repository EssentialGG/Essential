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

//#if MC<=11202
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.gui.overlay.OverlayManagerImpl;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiTextField.class)
public abstract class Mixin_UnfocusTextFieldWhileOverlayHasFocus {
    @ModifyExpressionValue(method = "drawTextBox", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiTextField;isFocused:Z"))
    private boolean suppressFocusWhenOverlayHasFocus(boolean isFocused) {
        if (OverlayManagerImpl.INSTANCE.getFocusedLayer() != null) {
            return false;
        } else {
            return isFocused;
        }
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_UnfocusTextFieldWhileOverlayHasFocus   {
//$$ }
//#endif
