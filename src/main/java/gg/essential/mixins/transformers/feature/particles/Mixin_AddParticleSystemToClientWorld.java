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
package gg.essential.mixins.transformers.feature.particles;

import gg.essential.handlers.EssentialSoundManager;
import gg.essential.mixins.ext.client.ParticleSystemHolder;
import gg.essential.model.ParticleSystem;
import gg.essential.model.backend.minecraft.WorldCollisionProvider;
import gg.essential.model.backend.minecraft.WorldLightProvider;
import kotlin.Unit;
import kotlin.random.Random;
import net.minecraft.client.multiplayer.WorldClient;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldClient.class)
public abstract class Mixin_AddParticleSystemToClientWorld implements ParticleSystemHolder {
    @Unique
    private final ParticleSystem particleSystem = new ParticleSystem(
        Random.Default,
        new WorldCollisionProvider((WorldClient) (Object) this),
        new WorldLightProvider((WorldClient) (Object) this),
        soundEvent -> {
            EssentialSoundManager.INSTANCE.playSound(soundEvent);
            return Unit.INSTANCE;
        }
    );

    @NotNull
    @Override
    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }
}
