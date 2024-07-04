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
import gg.essential.mixins.impl.client.audio.ISoundExt;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundManager.class)
public abstract class Mixin_ISoundExt_maxDistance {
    @ModifyExpressionValue(method = "playSound", at = @At(value = "CONSTANT", args = "floatValue=16"))
    private float applyMaxDistance(float maxDistance, ISound sound) {
        if (sound instanceof ISoundExt) {
            maxDistance = ((ISoundExt) sound).essential$maxDistance();
        }
        return maxDistance;
    }
}
