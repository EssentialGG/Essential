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
package gg.essential.mixins.transformers.compatibility.notenoughanimations;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.handlers.RenderPlayerBypass;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.tr7zw.notenoughanimations.logic.ArmTransformer")
public class MixinArmTransformer {
    @Dynamic("Compatibility with NotEnoughAnimations")
    @Inject(method = {"updateArms", "lambda$enable$1(Ldev/tr7zw/transliterationlib/api/wrapper/item/Arm;Ldev/tr7zw/transliterationlib/api/wrapper/item/Hand;Ldev/tr7zw/transliterationlib/api/wrapper/entity/LivingEntity;Ldev/tr7zw/transliterationlib/api/wrapper/model/PlayerEntityModel;FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"}, at = @At("HEAD"), remap = false, cancellable = true)
    private void disableOnEmulatedPlayer(CallbackInfo ci) {
        if (RenderPlayerBypass.bypass) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = {
        "lambda$enable$1(Ldev/tr7zw/transliterationlib/api/wrapper/item/Arm;Ldev/tr7zw/transliterationlib/api/wrapper/item/Hand;Ldev/tr7zw/transliterationlib/api/wrapper/entity/LivingEntity;Ldev/tr7zw/transliterationlib/api/wrapper/model/PlayerEntityModel;FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"
    }, at = @At(value = "FIELD", target = "Ldev/tr7zw/notenoughanimations/config/Config;enableAnimationSmoothing:Z"), remap = false)
    private boolean essential$disableSmoothingForEmotes(
        boolean original,
        @Coerce Object arm,
        @Coerce Object hand,
        @Coerce Object livingEntity,
        @Coerce Object playerEntityModel,
        float tick,
        CallbackInfo ci // not a mistake, this is captured by the lambda we target
    ) {
        Object entity = ((AbstractWrapperAccessor) livingEntity).essential$getHandler();
        if (entity instanceof AbstractClientPlayerExt && ((AbstractClientPlayerExt) entity).isPoseModified()) {
            return false;
        }
        return original;
    }
}
