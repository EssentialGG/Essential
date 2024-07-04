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

import gg.essential.config.EssentialConfig;
import net.minecraft.client.gui.ServerListEntryLanScan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerListEntryLanScan.class)
public abstract class MixinServerListEntryLanScan {
    @Inject(method = "drawEntry", at = @At("HEAD"), cancellable = true)
    private void skipForEssentialTabs(CallbackInfo ci) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return;
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) {
            ci.cancel();
        }
    }
}
