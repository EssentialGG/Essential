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
package gg.essential.mixins.transformers.feature.sound;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import paulscode.sound.Library;
import paulscode.sound.Source;

@Mixin(value = Library.class, remap = false)
public abstract class Mixin_FixPaulscodeChannelAllocation {
    // By default this method will re-allocate paused channels as if they were unused.
    // This is not desirable because it means that paused in-game sounds may disappear if you play enough sounds in the
    // menu.
    // This injector fixes that by treating paused channels the same way as playing ones.
    @ModifyExpressionValue(method = "getNextChannel", at = @At(value = "INVOKE", target = "Lpaulscode/sound/Source;playing()Z"))
    private boolean orPaused(boolean playing, @Local(name = "src") Source src) {
        return playing || src.paused();
    }
}
