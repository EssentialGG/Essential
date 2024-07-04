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

import gg.essential.mixins.impl.client.audio.ISoundExt;
import net.minecraft.client.audio.ChannelManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundEngine;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class Mixin_UpdateWhilePaused {
    // MC doesn't tick sounds when the game is paused, as such we can't cancel them or update their volume, both of
    // which we need to be able to do.
    @Inject(method = "tick", at = @At("RETURN"))
    private void updateEssentialSounds(boolean isGamePaused, CallbackInfo ci) {
        if (!isGamePaused) return;

        for (ITickableSound sound : this.tickableSounds) {
            if (!(sound instanceof ISoundExt)) {
                continue;
            }

            sound.tick();

            if (!sound.isDonePlaying()) {
                float volume = this.getClampedVolume(sound);
                float pitch = this.getClampedPitch(sound);
                Vector3d pos = new Vector3d(sound.getX(), sound.getY(), sound.getZ());
                ChannelManager.Entry channel = this.playingSoundsChannel.get(sound);
                if (channel != null) {
                    channel.runOnSoundExecutor((arg) -> {
                        arg.setGain(volume);
                        arg.setPitch(pitch);
                        arg.updateSource(pos);
                    });
                }
            } else {
                this.stop(sound);
            }
        }
    }

    @Shadow @Final private Map<ISound, ChannelManager.Entry> playingSoundsChannel;
    @Shadow @Final private List<ITickableSound> tickableSounds;

    @Shadow public abstract void stop(ISound sound);
    @Shadow protected abstract float getClampedVolume(ISound soundIn);
    @Shadow protected abstract float getClampedPitch(ISound soundIn);
}
