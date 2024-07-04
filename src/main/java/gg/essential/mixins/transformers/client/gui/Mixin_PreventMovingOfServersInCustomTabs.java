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
import gg.essential.config.EssentialConfig;
import net.minecraft.client.gui.GuiMultiplayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiMultiplayer.class)
public abstract class Mixin_PreventMovingOfServersInCustomTabs {
    @Inject(method = "canMoveUp", at = @At("HEAD"), cancellable = true)
    public void canMoveUp(CallbackInfoReturnable<Boolean> returnable) {
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) returnable.setReturnValue(false);
    }

    @Inject(method = "canMoveDown", at = @At("HEAD"), cancellable = true)
    public void canMoveDown(CallbackInfoReturnable<Boolean> returnable) {
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) returnable.setReturnValue(false);
    }


    @ModifyExpressionValue(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMultiplayer;isShiftKeyDown()Z"))
    public boolean cancelShift(boolean original) {
        return EssentialConfig.INSTANCE.getCurrentMultiplayerTab() == 0 && original;
    }
}
