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
import org.ice4j.StunResponseEvent;
import org.ice4j.attribute.TransactionTransmitCounterAttribute;
import org.ice4j.ice.CandidatePair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "org.ice4j.ice.ConnectivityCheckClient", remap = false)
public abstract class Mixin_StoreRttInCandidatePair {

    @ModifyVariable(method = "processSuccessResponse", name = "checkedPair", at = @At(value = "INVOKE", target = "Lorg/ice4j/ice/CandidatePair;setStateSucceeded()V"))
    private CandidatePair storeInCheckedPair(CandidatePair pair, StunResponseEvent event) {
        TransactionTransmitCounterAttribute attribute = TransactionTransmitCounterAttribute.get(event.getResponse());

        if (attribute != null && attribute.rtt != null) {
            ((CandidatePairExt) pair).setRtt(attribute.rtt);
        }

        return pair;
    }

    @ModifyVariable(method = "processSuccessResponse", name = "validPair", at = @At(value = "INVOKE", target = "Lorg/ice4j/ice/CandidatePair;setStateSucceeded()V"))
    private CandidatePair storeInValidPair(CandidatePair pair, StunResponseEvent event) {
        TransactionTransmitCounterAttribute attribute = TransactionTransmitCounterAttribute.get(event.getResponse());

        if (attribute != null && attribute.rtt != null) {
            ((CandidatePairExt) pair).setRtt(attribute.rtt);
        }

        return pair;
    }
}
