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
package gg.essential.mixins.transformers.compatibility.vanilla;

import gg.essential.mixins.transformers.client.gui.ServerSelectionListAccessor;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiMultiplayer.class)
public class Mixin_FixKeyboardNavigation {

    // There is a mistake in the vanilla code that skips dividers, it calls getSize() instead of getSelected()
    @Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ServerSelectionList;getSize()I", ordinal = 0))
    public int fixSkippingDividersUp(ServerSelectionList instance) {
        return instance.getSelected();
    }

    // There is a mistake in the vanilla code that skips dividers, it calls getSize() instead of getSelected()
    @Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ServerSelectionList;getSize()I", ordinal = 3))
    public int fixSkippingDividersDown(ServerSelectionList instance) {
        return instance.getSelected();
    }

    // There is an off by one in the vanilla code, it should be `i < this.serverListSelector.getSize() - 1`
    // not `i < this.serverListSelector.getSize()`.
    @Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ServerSelectionList;getSize()I", ordinal = 1))
    public int fixOffByOne(ServerSelectionList instance) {
        return ((ServerSelectionListAccessor) instance).invokeGetSize() - 1;
    }
}
