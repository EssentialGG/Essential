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
package gg.essential.mixins.transformers.feature.ice.common.ice4j;

import org.ice4j.ice.Agent;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.RelayedCandidate;
import org.ice4j.stack.StunStack;
import org.jitsi.utils.logging2.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ice4J keeps all relay candidates allocated for the entirety of the lifetime of the Agent, even if the selected pair
 * does not need them. This uses up limited resources (ports) on the TURN server.
 *
 * This mixin fixes that by closing all relay candidates that weren't selected once the ICE state processing is
 * completed (we don't need to handle the failure case, Ice4J already cleans up in that case).
 */
@Mixin(value = Agent.class, remap = false)
public abstract class Mixin_CleanupUnusedRelayCandidates {
    @Shadow public abstract List<IceMediaStream> getStreams();

    @Shadow public abstract StunStack getStunStack();

    @Shadow @Final private Logger logger;

    @Inject(method = "checkListStatesUpdated", at = @At(value = "INVOKE", target = "Lorg/ice4j/ice/Agent;scheduleStunKeepAlive()V"))
    private void closeRelayCandidatesWeNoLongerNeed(CallbackInfo ci) {
        Set<RelayedCandidate> usedRelayCandidates = new HashSet<>();
        Set<RelayedCandidate> allRelayCandidates = new HashSet<>();
        for (IceMediaStream stream : getStreams()) {
            for (Component component : stream.getComponents()) {
                CandidatePair selectedPair = component.getSelectedPair();
                if (selectedPair != null) {
                    LocalCandidate selectedCandidate = selectedPair.getLocalCandidate();
                    if (selectedCandidate instanceof RelayedCandidate) {
                        usedRelayCandidates.add((RelayedCandidate) selectedCandidate);
                    }
                }
                for (LocalCandidate localCandidate : component.getLocalCandidates()) {
                    if (localCandidate instanceof RelayedCandidate) {
                        allRelayCandidates.add((RelayedCandidate) localCandidate);
                    }
                }
            }
        }
        for (RelayedCandidate candidate : allRelayCandidates) {
            if (!usedRelayCandidates.contains(candidate)) {
                logger.debug("Closing " + candidate.toShortString() + " because we no longer need it.");
                getStunStack().removeSocket(candidate.getTransportAddress());
            }
        }
    }
}
