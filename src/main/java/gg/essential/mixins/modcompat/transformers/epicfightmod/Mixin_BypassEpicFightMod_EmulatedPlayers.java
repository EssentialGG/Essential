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
package gg.essential.mixins.modcompat.transformers.epicfightmod;

import gg.essential.handlers.RenderPlayerBypass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"maninthehouse.epicfight.client.events.engine.RenderEngine$Events", "yesman.epicfight.client.events.engine.RenderEngine$Events"})
public class Mixin_BypassEpicFightMod_EmulatedPlayers {
    @Inject(method = "renderLivingEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideHealthBarOnEmulatedPlayers(CallbackInfo ci) {
        if (RenderPlayerBypass.bypass) {
            ci.cancel();
        }
    }
}
