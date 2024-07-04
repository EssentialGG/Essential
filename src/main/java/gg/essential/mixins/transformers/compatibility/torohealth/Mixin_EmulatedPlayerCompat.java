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
package gg.essential.mixins.transformers.compatibility.torohealth;

import gg.essential.handlers.RenderPlayerBypass;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.torocraft.torohealth.util.EntityUtil")
public class Mixin_EmulatedPlayerCompat {
    @Inject(method = "showHealthBar", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideBarOnEmulatedPlayers(Entity entity, Minecraft client, CallbackInfoReturnable<Boolean> cir) {
        if (RenderPlayerBypass.bypass) {
            cir.setReturnValue(false);
        }
    }
}
