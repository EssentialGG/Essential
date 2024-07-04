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

import dev.folomeev.kotgl.matrix.vectors.Vec3;
import gg.essential.mixins.impl.client.audio.SoundSystemExt;
import gg.essential.model.util.Quaternion;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundHandler.class)
public abstract class Mixin_SoundSystemExt_SoundHandler implements SoundSystemExt {

    @Shadow @Final private SoundManager sndManager;

    @Nullable
    @Override
    public Vec3 essential$getListenerPosition() {
        return ((SoundSystemExt) this.sndManager).essential$getListenerPosition();
    }

    @Nullable
    @Override
    public Quaternion essential$getListenerRotation() {
        return ((SoundSystemExt) this.sndManager).essential$getListenerRotation();
    }
}
