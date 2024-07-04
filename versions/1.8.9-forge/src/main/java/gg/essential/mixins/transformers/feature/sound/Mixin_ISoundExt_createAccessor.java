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
import gg.essential.mixins.impl.client.audio.ISoundExt;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundManager.class)
public class Mixin_ISoundExt_createAccessor {
    private static final String GET_SOUND = "Lnet/minecraft/client/audio/SoundHandler;getSound(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/audio/SoundEventAccessorComposite;";

    @ModifyExpressionValue(method = "playSound", at = @At(value = "INVOKE", target = GET_SOUND))
    private SoundEventAccessorComposite resolveEssentialSoundAccessor(SoundEventAccessorComposite result, ISound sound) {
        if (result == null && sound instanceof ISoundExt) {
            result = ((ISoundExt) sound).essential$createAccessor();
        }
        return result;
    }

    @ModifyExpressionValue(method = "updateAllSounds", at = @At(value = "INVOKE", target = GET_SOUND, ordinal = 0))
    private SoundEventAccessorComposite resolveEssentialSoundAccessor$0(SoundEventAccessorComposite result, @Local ITickableSound sound) {
        if (result == null && sound instanceof ISoundExt) {
            result = ((ISoundExt) sound).essential$createAccessor();
        }
        return result;
    }

    @ModifyExpressionValue(method = "updateAllSounds", at = @At(value = "INVOKE", target = GET_SOUND, ordinal = 1))
    private SoundEventAccessorComposite resolveEssentialSoundAccessor$1(SoundEventAccessorComposite result, @Local ISound sound) {
        if (result == null && sound instanceof ISoundExt) {
            result = ((ISoundExt) sound).essential$createAccessor();
        }
        return result;
    }
}
