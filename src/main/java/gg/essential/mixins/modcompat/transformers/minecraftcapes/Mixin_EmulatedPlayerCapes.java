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
package gg.essential.mixins.modcompat.transformers.minecraftcapes;

import gg.essential.gui.common.EmulatedUI3DPlayer.EmulatedPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraftcapes.player.render.CapeLayer")
public class Mixin_EmulatedPlayerCapes {
    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideCapeOnEmulatedPlayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (player instanceof EmulatedPlayer) {
            EmulatedPlayer emulatedPlayer = (EmulatedPlayer) player;
            if (!emulatedPlayer.getEmulatedUI3DPlayer().getShowCape().get()) {
                ci.cancel();
            }
        }
    }
}
