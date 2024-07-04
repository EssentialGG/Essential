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

import org.ice4j.ice.harvest.StunCandidateHarvest;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StunCandidateHarvest.class, remap = false)
public abstract class Mixin_FixIce4JKeepAliveScheduling {
    @Shadow
    private long sendKeepAliveMessageTime = -1;

    @Inject(method = "runInSendKeepAliveMessageThread", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lorg/ice4j/ice/harvest/StunCandidateHarvest;sendKeepAliveMessageTime:J"))
    private void initializeSendKeepAliveMessageTime(CallbackInfoReturnable<Boolean> ci) {
        if (sendKeepAliveMessageTime == -1) {
            sendKeepAliveMessageTime = System.currentTimeMillis();
        }
    }
}
