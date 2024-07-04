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
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.tr7zw.notenoughanimations.logic.PlayerTransformer")
public class MixinPlayerTransformer {
    @ModifyExpressionValue(method = "updateModel", at = @At(value = "FIELD", target = "Ldev/tr7zw/notenoughanimations/config/Config;enableAnimationSmoothing:Z"), remap = false)
    private boolean essential$disableSmoothingForEmotes(boolean original, AbstractClientPlayer player) {
        if (((AbstractClientPlayerExt) player).isPoseModified()) {
            return false;
        }
        return original;
    }
}
