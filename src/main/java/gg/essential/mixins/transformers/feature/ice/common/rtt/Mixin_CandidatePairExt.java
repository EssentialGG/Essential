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
package gg.essential.mixins.transformers.feature.ice.common.rtt;

import gg.essential.mixins.impl.feature.ice.common.rtt.CandidatePairExt;
import org.ice4j.ice.CandidatePair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.time.Duration;

@Mixin(value = CandidatePair.class, remap = false)
public abstract class Mixin_CandidatePairExt implements CandidatePairExt {

    @Unique
    private Duration rtt;

    @Override
    public Duration getRtt() {
        return this.rtt;
    }

    @Override
    public void setRtt(Duration rtt) {
        this.rtt = rtt;
    }
}
