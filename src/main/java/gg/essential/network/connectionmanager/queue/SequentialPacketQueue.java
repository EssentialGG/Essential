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
package gg.essential.network.connectionmanager.queue;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.util.Multithreading;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SequentialPacketQueue implements PacketQueue {

    private static final long ATTEMPT_RESEND_SECONDS = 1L;
    @NotNull
    private final ConnectionManager connectionManager;
    @NotNull
    private final TimeoutPolicy timeoutPolicy;

    @NotNull
    private final Queue<Pair<Packet, Consumer<Optional<Packet>>>> queue = new ArrayDeque<>();

    @Nullable
    private Pair<Packet, Consumer<Optional<Packet>>> waitingFor;

    public SequentialPacketQueue(
            @NotNull ConnectionManager connectionManager,
            @NotNull TimeoutPolicy timeoutPolicy
    ) {
        this.connectionManager = connectionManager;
        this.timeoutPolicy = timeoutPolicy;
    }

    @Override
    public synchronized void enqueue(Packet packet, Consumer<Optional<Packet>> responseCallback) {
        this.queue.add(new Pair<>(packet, responseCallback));
        this.process();
    }

    private void process() {
        if (this.waitingFor != null) {
            return;
        }

        Pair<Packet, Consumer<Optional<Packet>>> next = this.queue.poll();
        if (next == null) {
            return;
        }
        this.waitingFor = next;
        this.attemptSend(next);
    }

    private synchronized void attemptSend(@NotNull final Pair<Packet, Consumer<Optional<Packet>>> next) {
        if (this.connectionManager.isOpen() && this.connectionManager.isAuthenticated()) {
            this.connectionManager.send(next.getFirst(), resp -> handleResponse(next, resp));
        } else if (timeoutPolicy == TimeoutPolicy.RETRANSMIT) {
            Multithreading.getScheduledPool().schedule(() -> attemptSend(next), ATTEMPT_RESEND_SECONDS, TimeUnit.SECONDS);
        } else {
            handleResponse(next, Optional.empty());
        }
    }

    private synchronized void handleResponse(
            @NotNull
            Pair<Packet, Consumer<Optional<Packet>>> request,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Packet> response
    ) {
        if (this.waitingFor != request) {
            return; // this can happen if we already dealt with this packet because of a reconnect
        }

        if (response.isPresent() || this.timeoutPolicy == TimeoutPolicy.SKIP) {
            this.waitingFor = null;
            Consumer<Optional<Packet>> responseCallback = request.getSecond();
            if (responseCallback != null) {
                responseCallback.accept(response);
            }
        } else { // TimeoutPolicy.RETRANSMIT
            this.attemptSend(request);
        }
        this.process();
    }

    @Override
    public synchronized void reset() {
        this.queue.clear();
        this.waitingFor = null;
    }

    public enum TimeoutPolicy {
        /**
         * Re-transmit any packet until it succeeds.
         * Only use if packets are idempotent (i.e. duplicate transmission does not change outcome).
         */
        RETRANSMIT,

        /**
         * Forwards the timeout to the given response callback and continues with the next packet.
         */
        SKIP,
    }

    public static class Builder {
        private final @NotNull ConnectionManager connectionManager;
        private @NotNull TimeoutPolicy timeoutPolicy = TimeoutPolicy.RETRANSMIT;

        public Builder(@NotNull ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
        }

        public Builder onTimeoutRetransmit() {
            return this.setTimeoutPolicy(TimeoutPolicy.RETRANSMIT);
        }

        public Builder onTimeoutSkip() {
            return this.setTimeoutPolicy(TimeoutPolicy.SKIP);
        }

        public Builder setTimeoutPolicy(@NotNull TimeoutPolicy timeoutPolicy) {
            this.timeoutPolicy = timeoutPolicy;
            return this;
        }

        public SequentialPacketQueue create() {
            return new SequentialPacketQueue(connectionManager, timeoutPolicy);
        }
    }
}
