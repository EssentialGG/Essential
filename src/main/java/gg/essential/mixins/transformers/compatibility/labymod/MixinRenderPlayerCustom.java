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
package gg.essential.mixins.transformers.compatibility.labymod;

//#if MC<=11202
import gg.essential.mixins.transformers.client.renderer.entity.MixinRenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.labymod.mojang.RenderPlayerHook$RenderPlayerCustom")
@SuppressWarnings("UnresolvedMixinReference")
public abstract class MixinRenderPlayerCustom extends MixinRenderPlayer {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void initEssentialCosmeticsLayer(CallbackInfo ci) {
        layerRenderers.add(super.essentialModelRenderer); // Already initialized in the parent constructor
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class MixinRenderPlayerCustom   {
//$$ }
//#endif
