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
package gg.essential.network.connectionmanager.ice;

import gg.essential.mixins.impl.feature.ice.common.AgentExt;
import gg.essential.mixins.impl.feature.ice.common.rtt.CandidatePairExt;
import gg.essential.util.Multithreading;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.CandidatePairState;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NominateBestRTT implements PropertyChangeListener {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Maximum time to wait after finding the first candidate if no direct pairs have been found yet.
     * If this timer runs out, we'll go with one of the relays we found.
     * If we find a direct pair, {@link #WAIT_FOR_MORE_DIRECTS} is started in addition to this timer.
     *
     * Put another way, this is how long we are willing to stall for to avoid relays.
     */
    private static final Duration WAIT_FOR_DIRECTS = Duration.of(
        Integer.getInteger("essential.sps.wait_for_directs", 10_000), ChronoUnit.MILLIS);

    /**
     * Maximum time to wait after finding the first direct candidate.
     * In case there are other routes with better RTT.
     */
    private static final Duration WAIT_FOR_MORE_DIRECTS = Duration.of(
        Integer.getInteger("essential.sps.wait_for_more_directs", 3_000), ChronoUnit.MILLIS);

    /**
     * We generally want to avoid relaying cause that has bandwidth costs but if the direct path is more than Xms slower
     * than the relay (likely because of bad routing), then we'll take the relay anyway.
     */
    private static final Duration RELAY_RTT_THRESHOLD = Duration.of(
        Integer.getInteger("essential.sps.relay_latency_threshold", 100), ChronoUnit.MILLIS);

    // FIXME these assume only a single component (true for all our use cases but noteworthy if this gets reused)
    private Instant firstDirect;
    private Instant firstRelay;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        String propertyName = evt.getPropertyName();

        if (source instanceof Agent) {
            if (Agent.PROPERTY_ICE_PROCESSING_STATE.equals(propertyName)) {
                iceProcessingStateChange((Agent) source, (IceProcessingState) evt.getNewValue());
            }
            if (AgentExt.PROPERTY_REMOTE_TRICKLING_DONE.equals(propertyName)) {
                Agent agent = (Agent) source;
                if (!agent.isControlling()) {
                    return; // only the controlling agent gets to nominate candidate pairs
                }
                for (IceMediaStream stream : agent.getStreams()) {
                    nominatePair(stream);
                }
            }
        }

        if (source instanceof CandidatePair) {
            if (IceMediaStream.PROPERTY_PAIR_VALIDATED.equals(propertyName)) {
                // Pair has validated, if it was the last one, we might be done now
                candidatePairStateChanged((CandidatePair) source);
            }
            if (IceMediaStream.PROPERTY_PAIR_STATE_CHANGED.equals(propertyName) && evt.getNewValue() == CandidatePairState.FAILED) {
                // Pair has failed, if it was the last one, we might be done now
                candidatePairStateChanged((CandidatePair) source);
            }
        }
    }

    private void iceProcessingStateChange(Agent agent, IceProcessingState newState) {
        if (newState == IceProcessingState.RUNNING) {
            // Subscribe to all streams so get notified when candidate pairs are validated/failed
            for (IceMediaStream stream : agent.getStreams()) {
                stream.addPairChangeListener(this);
            }
        }
    }

    private void candidatePairStateChanged(CandidatePair pair) {
        Component component = pair.getParentComponent();
        IceMediaStream stream = component.getParentStream();

        if (!stream.getParentAgent().isControlling()) {
            return; // only the controlling agent gets to nominate candidate pairs
        }

        nominatePair(stream);
    }

    private void nominatePair(IceMediaStream stream) {
        Set<CandidatePair> allValidPairs = getValidPairs(stream);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (allValidPairs) {
            nominatePair(stream, allValidPairs);
        }
    }

    private synchronized void nominatePair(IceMediaStream stream, Set<CandidatePair> allValidPairs) {
        for (Component component : stream.getComponents()) {
            Collection<CandidatePair> validPairs = allValidPairs.stream()
                .filter(it -> it.getParentComponent() == component)
                .collect(Collectors.toList());

            if (validPairs.stream().anyMatch(CandidatePair::isNominated)) {
                continue; // already have a nominated pair for this component
            }

            CandidatePair bestDirectPair = null;
            Duration bestDirectRtt = null;
            CandidatePair bestRelayPair = null;
            Duration bestRelayRtt = null;

            for (CandidatePair pair : allValidPairs) {

                Duration rtt = ((CandidatePairExt) pair).getRtt();
                if (rtt == null) {
                    rtt = Duration.ofMinutes(1); // prefer candidates for which we actually have data
                }

                boolean isRelay = pair.getLocalCandidate().getType() == CandidateType.RELAYED_CANDIDATE
                    || pair.getRemoteCandidate().getType() == CandidateType.RELAYED_CANDIDATE;
                if (isRelay) {
                    if (bestRelayRtt == null || bestRelayRtt.compareTo(rtt) > 0) {
                        bestRelayRtt = rtt;
                        bestRelayPair = pair;
                    }
                } else {
                    if (bestDirectRtt == null || bestDirectRtt.compareTo(rtt) > 0) {
                        bestDirectRtt = rtt;
                        bestDirectPair = pair;
                    }
                }
            }

            if (bestDirectPair == null && bestRelayPair == null) {
                continue; // no valid pairs found for this component, try again later
            }

            // Once we find the first candidates, arm the timers
            if (bestDirectPair != null && firstDirect == null) {
                firstDirect = Instant.now();
                Multithreading.getScheduledPool().schedule(() -> {
                    nominatePair(stream);
                }, WAIT_FOR_MORE_DIRECTS.toNanos(), TimeUnit.NANOSECONDS);
            }
            if (bestRelayPair != null && firstRelay == null) {
                firstRelay = Instant.now();
                Multithreading.getScheduledPool().schedule(() -> {
                    nominatePair(stream);
                }, WAIT_FOR_DIRECTS.toNanos(), TimeUnit.NANOSECONDS);
            }

            if (!stream.getCheckList().allChecksCompleted() || !((AgentExt) stream.getParentAgent()).isRemoteTricklingDone()) {
                // checks are still in progress, maybe wait for more results to come in
                if (firstDirect == null) {
                    // no direct candidates found yet, stall for time in hope of finding one
                    if (Instant.now().isBefore(firstRelay.plus(WAIT_FOR_DIRECTS))) {
                        continue;
                    } else {
                        LOGGER.info("Waited {}ms for direct candidate pairs to no avail, going ahead with nomination..",
                            WAIT_FOR_DIRECTS.toMillis());
                    }
                } else {
                    // we've got a direct pair but maybe there are better ones out there
                    if (Instant.now().isBefore(firstDirect.plus(WAIT_FOR_MORE_DIRECTS))) {
                        continue;
                    } else {
                        LOGGER.info("Waited {}ms for more direct candidates, going ahead with nomination..",
                            WAIT_FOR_MORE_DIRECTS.toMillis());
                    }
                }
            } else {
                LOGGER.info("All checks have been completed, going ahead with nomination..");
            }

            CandidatePair nominatedPair;
            if (bestDirectPair == null) {
                nominatedPair = bestRelayPair;
            } else if (bestRelayPair == null) {
                nominatedPair = bestDirectPair;
            } else {
                if (bestRelayRtt.plus(RELAY_RTT_THRESHOLD).compareTo(bestDirectRtt) < 0) {
                    nominatedPair = bestRelayPair;
                } else {
                    nominatedPair = bestDirectPair;
                }
            }

            LOGGER.info("Nominate (best rtt): {}", toShortString(nominatedPair));
            for (CandidatePair pair : validPairs) {
                String extra = "";
                if (pair == bestDirectPair) {
                    extra = " (best direct RTT)";
                } else if (pair == bestRelayPair) {
                    extra = " (best relay RTT)";
                }
                LOGGER.info(" {} {}{}", pair == nominatedPair ? "*" : " ", toShortString(pair), extra);
            }
            stream.getParentAgent().nominate(nominatedPair);
        }
    }

    private String toShortString(CandidatePair pair) {
        if (pair == null) {
            return "null";
        }
        Duration rtt = ((CandidatePairExt) pair).getRtt();
        return MessageFormat.format("{0} -> {1} ({2}ms RTT)",
            pair.getLocalCandidate().toShortString(),
            pair.getRemoteCandidate().toShortString(),
            rtt == null ? "?" : rtt.toMillis());
    }

    @SuppressWarnings("unchecked")
    private TreeSet<CandidatePair> getValidPairs(IceMediaStream stream) {
        try {
            Field field = IceMediaStream.class.getDeclaredField("validList");
            field.setAccessible(true);
            return (TreeSet<CandidatePair>) field.get(stream);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}
