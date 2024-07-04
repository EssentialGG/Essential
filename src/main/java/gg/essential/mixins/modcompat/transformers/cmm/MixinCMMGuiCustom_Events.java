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
package gg.essential.mixins.modcompat.transformers.cmm;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import gg.essential.mixins.ext.compatibility.CMMGuiCustomExt;
import gg.essential.mixins.transformers.client.gui.MixinGuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "lumien.custommainmenu.gui.GuiCustom")
public class MixinCMMGuiCustom_Events extends MixinGuiScreen implements CMMGuiCustomExt {

    boolean essential$isMainMenu = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void essential$checkIfMainMenu(@Coerce CMMGuiConfigAccessor config, CallbackInfo ci) {
        if (config.getName().equals("mainmenu")) {
            this.essential$isMainMenu = true;
        }
    }

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("HEAD"))
    public void essential$drawScreenEvent(int mouseX, int mouseY, float partialTicks, CallbackInfo ci,
                                          @Local(ordinal = 0, argsOnly = true) LocalIntRef mouseXRef, @Local(ordinal = 1, argsOnly = true) LocalIntRef mouseYRef) {
        super.drawScreen(mouseX, mouseY, partialTicks, ci, mouseXRef, mouseYRef);
    }

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("TAIL"))
    public void essential$drawScreenEventPost(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        super.drawScreenPost(mouseX, mouseY, partialTicks, ci);
    }

    @Override
    public boolean essential$isMainMenu() {
        return this.essential$isMainMenu;
    }
}
