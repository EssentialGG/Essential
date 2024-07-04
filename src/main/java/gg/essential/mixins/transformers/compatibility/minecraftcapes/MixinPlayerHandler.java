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
package gg.essential.mixins.transformers.compatibility.minecraftcapes;

//#if MC>11700
//$$ import gg.essential.gui.common.EmulatedUI3DPlayer;
//$$ import net.minecraft.client.network.AbstractClientPlayerEntity;
//$$ import net.minecraft.client.render.VertexConsumerProvider;
//$$ import net.minecraft.client.util.math.MatrixStack;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Pseudo;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Pseudo
//$$ @Mixin(targets = {"net.minecraftcapes.player.render.CapeLayer", "net.minecraftcapes.player.render.Deadmau5"})
//$$ public abstract class MixinPlayerHandler {
//$$
//$$     @Inject(method = "render", at = @At("HEAD"), remap = false, cancellable = true)
//$$     private void essential$disableCapes(
//$$         MatrixStack matrixStack,
//$$         VertexConsumerProvider vertexConsumerProvider,
//$$         int i,
//$$         AbstractClientPlayerEntity abstractClientPlayer,
//$$         float f,
//$$         float g,
//$$         float h,
//$$         float j,
//$$         float k,
//$$         float l,
//$$         CallbackInfo ci
//$$     ) {
//$$         if (abstractClientPlayer instanceof EmulatedUI3DPlayer.EmulatedPlayer && !((EmulatedUI3DPlayer.EmulatedPlayer) abstractClientPlayer).getEmulatedUI3DPlayer().getShowCape().get()) ci.cancel();
//$$     }
//$$ }
//#else
@org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
public abstract class MixinPlayerHandler {}
//#endif
