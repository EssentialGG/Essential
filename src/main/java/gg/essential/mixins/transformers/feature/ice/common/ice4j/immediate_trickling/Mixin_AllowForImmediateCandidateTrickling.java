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
package gg.essential.mixins.transformers.feature.ice.common.ice4j.immediate_trickling;

import gg.essential.mixins.impl.feature.ice.common.TricklingCandidateHarvester;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.ice4j.ice.harvest.TrickleCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "org.ice4j.ice.harvest.CandidateHarvesterSetElement", remap = false)
public abstract class Mixin_AllowForImmediateCandidateTrickling {
    @Shadow
    @Final
    private CandidateHarvester harvester;

    @ModifyVariable(method = "harvest", at = @At("HEAD"), argsOnly = true)
    private TrickleCallback forwardTrickleCallbackToHarvester(TrickleCallback trickleCallback) {
        CandidateHarvester harvester = this.harvester;
        if (harvester instanceof TricklingCandidateHarvester) {
            ((TricklingCandidateHarvester) harvester).setTrickleCallback(trickleCallback);
            return null;
        } else {
            return trickleCallback;
        }
    }
}
