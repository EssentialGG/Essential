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
// MC<1.17
package gg.essential.mixins.transformers.cosmetics.skinmask;

import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemRenderer.class)
public abstract class Mixin_ApplyToHandRenderer {
    @Shadow
    @Final
    private Minecraft mc;

    @ModifyArg(
        method = {"renderArm", "renderArmFirstPerson"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V")
    )
    private ResourceLocation applyCosmeticsSkinMask(ResourceLocation skin) {
        return ((AbstractClientPlayerExt) this.mc.player).applyEssentialCosmeticsMask(skin);
    }
}
