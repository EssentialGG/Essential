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

import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.CandidatePairState;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.CheckList;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.RemoteCandidate;
import org.jitsi.utils.logging2.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ice4J fails to pair up locally trickled candidates with existing remote candidates, thereby failing to check some
 * potentially valid pairs.
 */
@Mixin(value = Component.class, remap = false)
public abstract class Mixin_FixIce4JLocalTrickling {
    @Shadow
    @Final
    private List<RemoteCandidate> remoteCandidates;

    @Shadow
    @Final
    private IceMediaStream parentStream;

    @Shadow
    @Final
    private Logger logger;

    // This method pairs newly gathered local candidates as described in
    // https://www.rfc-editor.org/rfc/rfc8838.html#name-pairing-newly-gathered-loca
    // Comments with extra indention are quotes from this RFC (unless noted otherwise).
    @Inject(method = "addLocalCandidate", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void computeNewPairs(LocalCandidate localCnd, CallbackInfoReturnable<Boolean> ci) {
        // 1. A Trickle ICE agent MUST NOT pair a local candidate until it has been trickled to the remote party.
        // Trickling to the remote party is the one thing Ice4J already does correctly

        // 2. Once the agent has conveyed the local candidate to the remote party, the agent checks if any remote
        //    candidates are currently known for this same stream and component.
        //    If not, the agent merely adds the new candidate to the list of local candidates (without pairing it).
        if (remoteCandidates.isEmpty()) {
            return; // nothing to do, just continue and Ice4J will add it to the local candidates list
        }

        // (intentionally doing step 4 first because the local candidate is the same for all pairs)
        // 4. If a newly formed pair has a local candidate whose type is server-reflexive, the agent MUST replace the
        //    local candidate with its base before completing the relevant redundancy tests.
        if (localCnd.getType() == CandidateType.SERVER_REFLEXIVE_CANDIDATE) {
            localCnd = localCnd.getBase();
        }

        // 3. Otherwise, if the agent has already learned of one or more remote candidates for this stream and
        //    component, it attempts to pair the new local candidate as described in the ICE specification [RFC8445].
        List<CandidatePair> newPairs = new ArrayList<>();
        for (RemoteCandidate remoteCnd : remoteCandidates) {
            if (localCnd.canReach(remoteCnd) && remoteCnd.getTransportAddress().getPort() != 0) {
                newPairs.add(new CandidatePair(localCnd, remoteCnd));
            }
        }

        CheckList checkList = parentStream.getCheckList();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (checkList) {
            // 5. The agent prunes redundant pairs by following the rules in Section 6.1.2.4 of [RFC8445] but checks
            //    existing pairs only if they have a state of Waiting or Frozen; this avoids removal of pairs for which
            //    connectivity checks are in flight (a state of In‑Progress) or for which connectivity checks have
            //    already yielded a definitive result (a state of Succeeded or Failed).
            List<CandidatePair> toBeRemoved = new ArrayList<>();
            newPairs:
            for (CandidatePair newPair : newPairs) {
                for (CandidatePair existingPair : checkList) {
                    // From the referenced section 6.1.2.4:
                    //    This is done by removing a candidate pair if it is redundant with a higher-priority candidate
                    //    pair in the same checklist.  Two candidate pairs are redundant if their local candidates have
                    //    the same base and their remote candidates are identical.
                    boolean sameBase = existingPair.getLocalCandidate().getBase().equals(newPair.getLocalCandidate().getBase());
                    boolean sameRemote = existingPair.getRemoteCandidate().equals(newPair.getRemoteCandidate());
                    boolean redundant = sameBase && sameRemote;
                    if (redundant) {
                        if (existingPair.getPriority() >= newPair.getPriority()) {
                            logger.debug("ignoring new pair " + newPair.toShortString() +
                                " because of existing higher priority pair " + existingPair.toShortString());
                            continue newPairs;
                        } else {
                            // Only cancel pairs if they are yet to be checked, otherwise there is nothing to gain
                            CandidatePairState state = existingPair.getState();
                            if (state == CandidatePairState.FROZEN || state == CandidatePairState.WAITING) {
                                logger.debug("removing existing pair " + existingPair.toShortString() +
                                    " because of new higher priority pair " + newPair.toShortString());
                                toBeRemoved.add(existingPair);
                            }
                        }
                    }
                }
            }
            toBeRemoved.forEach(newPairs::remove);
            toBeRemoved.forEach(checkList::remove);

            int maxCheckListSize = ((IceMediaStreamAcc) parentStream).getMaxCheckListSize();

            // 6. If, after completing the relevant redundancy tests, the checklist where the pair is to be added already
            //    contains the maximum number of candidate pairs (100 by default as per [RFC8445]), the agent SHOULD discard
            //    any pairs in the Failed state to make room for the new pair. If there are no such pairs, the agent SHOULD
            //    discard a pair with a lower priority than the new pair in order to make room for the new pair,
            //    until the number of pairs is equal to the maximum number of pairs. This processing is consistent with
            //    Section 6.1.2.5 of [RFC8445].
            newPairs.sort(CandidatePair.comparator);
            for (CandidatePair newPair : newPairs) {
                // If the check list is full, try to remove failed pairs first
                if (checkList.size() + 1 > maxCheckListSize) {
                    for (CandidatePair existingPair : checkList) {
                        if (existingPair.getState() == CandidatePairState.FAILED) {
                            logger.debug("discarding failed pair " + existingPair.toShortString() +
                                " to make space for new pair");
                            checkList.remove(existingPair);
                            break;
                        }
                    }
                }

                // If the check list is still full, try to remove lower priority pairs
                if (checkList.size() + 1 > maxCheckListSize) {
                    // The specs says we SHOULD do this, not that we MUST.
                    // I doubt this will be helpful in practice, so I'll pass.
                }

                // If the check list is still full, we can't add the pair
                if (checkList.size() + 1 > maxCheckListSize) {
                    logger.info("ignoring new pair because checklist is full: " + newPair.toShortString());
                    continue;
                }

                // We've got space, add the newly discovered pair to the CheckList.
                checkList.add(newPair);
                logger.info("new Pair added: " + newPair.toShortString() + ".");
            }
        }
    }
}
