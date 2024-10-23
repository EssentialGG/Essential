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
package gg.essential.mixins.transformers.cosmetics.skinmask;

import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//#endif

@Mixin(RenderPlayer.class)
public abstract class Mixin_ApplyToPlayerRenderer {
    //#if MC>=12102
    //$$ @Inject(method = "getTexture(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Lnet/minecraft/util/Identifier;", at = @At("RETURN"), cancellable = true)
    //$$ private void applyCosmeticsSkinMask(PlayerEntityRenderState state, CallbackInfoReturnable<Identifier> ci) {
    //$$     AbstractClientPlayerEntity player = ((PlayerEntityRenderStateExt) state).essential$getEntity();
    //#else
    @Inject(method = "getEntityTexture(Lnet/minecraft/client/entity/AbstractClientPlayer;)Lnet/minecraft/util/ResourceLocation;", at = @At("RETURN"), cancellable = true)
    private void applyCosmeticsSkinMask(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> ci) {
    //#endif
        ci.setReturnValue(((AbstractClientPlayerExt) player).applyEssentialCosmeticsMask(ci.getReturnValue()));
    }
}
