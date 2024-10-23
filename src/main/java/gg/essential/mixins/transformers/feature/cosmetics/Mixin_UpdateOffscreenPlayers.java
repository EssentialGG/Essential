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
package gg.essential.mixins.transformers.feature.cosmetics;

import gg.essential.handlers.EssentialSoundManager;
import gg.essential.mixins.ext.client.ParticleSystemHolder;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.model.ModelAnimationState;
import gg.essential.model.ModelInstance;
import gg.essential.model.ParticleSystem;
import gg.essential.model.PlayerMolangQuery;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Updates cosmetic animations/events/effects of players that are currently off-screen (that is, have not been rendered
 * for some reason).
 */
@Mixin(RenderGlobal.class)
public abstract class Mixin_UpdateOffscreenPlayers {
    @Shadow
    private WorldClient world;

    // TODO 1.21.2 is this place still good? maybe separate update from render entirely
    //#if MC>=12102
    //$$ @Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=blockentities"))
    //#elseif MC>=11400
    //$$ @Inject(method = "updateCameraAndRender", at = @At(value = "CONSTANT", args = "stringValue=blockentities"))
    //#else
    @Inject(method = "renderEntities", at = @At("RETURN"))
    //#endif
    private void essential$updateCosmetics(CallbackInfo ci) {
        ParticleSystem particleSystem = this.world instanceof ParticleSystemHolder
            ? ((ParticleSystemHolder) this.world).getParticleSystem()
            : null;

        //#if MC>=11400
        //$$ for (PlayerEntity player : this.world.getPlayers()) {
        //#else
        for (EntityPlayer player : this.world.playerEntities) {
        //#endif
            if (!(player instanceof AbstractClientPlayerExt)) {
                continue;
            }
            AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) player;

            float lifeTime = new PlayerMolangQuery(player).getLifeTime();
            if (playerExt.getLastCosmeticsUpdateTime() >= lifeTime) {
                continue;
            }

            for (ModelInstance model : playerExt.getWearablesManager().getModels().values()) {
                model.getEssentialAnimationSystem().updateAnimationState();
                model.updateEffects();

                List<ModelAnimationState.Event> pendingEvents = model.getAnimationState().getPendingEvents();
                if (!pendingEvents.isEmpty()) {
                    for (ModelAnimationState.Event event : pendingEvents) {
                        if (event instanceof ModelAnimationState.ParticleEvent) {
                            if (particleSystem != null) {
                                particleSystem.spawn((ModelAnimationState.ParticleEvent) event);
                            }
                        } else if (event instanceof ModelAnimationState.SoundEvent) {
                            EssentialSoundManager.INSTANCE.playSound((ModelAnimationState.SoundEvent) event);
                        }
                    }
                    pendingEvents.clear();
                }
            }
        }
    }
}
