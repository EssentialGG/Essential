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
import gg.essential.handlers.RenderPlayerBypass;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.labymod.user.cosmetic.ModelCosmetics")
@SuppressWarnings("UnresolvedMixinReference")
public abstract class MixinModelCosmetics extends ModelPlayer {
    public MixinModelCosmetics(float modelSize, boolean smallArmsIn) {
        super(modelSize, smallArmsIn);
    }

    @Inject(method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void disableCosmeticsOnEmulatedPlayer(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float yaw, float pitch, float scale, CallbackInfo ci) {
        if (RenderPlayerBypass.bypass) {
            super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, yaw, pitch, scale);
            ci.cancel();
        }
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class MixinModelCosmetics {
//$$ }
//#endif
