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
// MC>=1.17
package gg.essential.mixins.transformers.cosmetics.skinmask;

import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class Mixin_ApplyToHandRenderer {
    private AbstractClientPlayerEntity player;

    @ModifyVariable(method = "renderArm", at = @At("HEAD"), argsOnly = true)
    private AbstractClientPlayerEntity storePlayer(AbstractClientPlayerEntity player) {
        this.player = player;
        return player;
    }

    @Inject(method = "renderArm", at = @At("RETURN"))
    private void clearPlayer(CallbackInfo ci) {
        this.player = null;
    }

    @ModifyArg(
        method = "renderArm",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getEntitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;")
    )
    private Identifier applyCosmeticsSkinMaskToArm(Identifier skin) {
        return ((AbstractClientPlayerExt) player).applyEssentialCosmeticsMask(skin);
    }

    @ModifyArg(
        method = "renderArm",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getEntityTranslucent(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;")
    )
    private Identifier applyCosmeticsSkinMaskToSleeve(Identifier skin) {
        return ((AbstractClientPlayerExt) player).applyEssentialCosmeticsMask(skin);
    }

    @Redirect(
        method = "renderArm",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getEntitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;")
    )
    private RenderLayer useTranslucentLayerForHand(Identifier texture) {
        return RenderLayer.getEntityTranslucent(texture);
    }
}
