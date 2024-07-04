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
package gg.essential.mixins.transformers.client.model;

import gg.essential.mixins.ext.client.model.geom.ExtraTransformHolder;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBase.class)
public abstract class Mixin_ExtraTransform_CopyBetweenModelParts {
    @Inject(method = "copyModelAngles", at = @At("RETURN"))
    private static void copyExtra(ModelRenderer source, ModelRenderer dest, CallbackInfo ci) {
        ((ExtraTransformHolder) dest).setExtra(((ExtraTransformHolder) source).getExtra());
    }
}
