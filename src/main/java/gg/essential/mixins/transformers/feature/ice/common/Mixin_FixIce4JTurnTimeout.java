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
package gg.essential.mixins.transformers.feature.ice.common;

import org.ice4j.ice.harvest.TurnCandidateHarvest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = TurnCandidateHarvest.class, remap = false)
public abstract class Mixin_FixIce4JTurnTimeout {
    @ModifyArg(method = "processSuccess", at = @At(value = "INVOKE", target = "Lorg/ice4j/ice/harvest/TurnCandidateHarvest;setSendKeepAliveMessageInterval(J)V"))
    private long sendRefreshOneMinuteBeforeEOL(long lifetime) {
        return lifetime == 0 ? 0 : Math.max(1_000, lifetime - 60_000);
    }
}
