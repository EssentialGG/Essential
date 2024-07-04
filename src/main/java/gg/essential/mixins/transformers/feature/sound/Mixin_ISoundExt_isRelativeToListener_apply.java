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

import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.Source;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

import static gg.essential.network.connectionmanager.ice.util.IWishMixinAllowedForPublicStaticFields.SOUND_RELATIVE_MARKER;

@Mixin(value = SourceLWJGLOpenAL.class, remap = false)
public abstract class Mixin_ISoundExt_isRelativeToListener_apply extends Source {
    @Shadow
    private ChannelLWJGLOpenAL channelOpenAL;

    @Shadow
    protected abstract boolean checkALError();

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lpaulscode/sound/libraries/LibraryLWJGLOpenAL;alPitchSupported()Z"))
    private void setRelativeToListener(CallbackInfo ci) {
        if (sourcename.contains(SOUND_RELATIVE_MARKER)) {
            AL10.alSourcei(channelOpenAL.ALSource.get(0), AL10.AL_SOURCE_RELATIVE, 1);
            checkALError();
        }
    }

    @Inject(method = "calculateDistance", at = @At("HEAD"), cancellable = true)
    private void fixDistanceCalculationWhenRelativeToListener(CallbackInfo ci) {
        if (sourcename.contains(SOUND_RELATIVE_MARKER)) {
            float x = position.x;
            float y = position.y;
            float z = position.z;
            distanceFromListener = (float) Math.sqrt(x*x + y*y + z*z);
            ci.cancel();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public Mixin_ISoundExt_isRelativeToListener_apply() { super(null, null); }
}
