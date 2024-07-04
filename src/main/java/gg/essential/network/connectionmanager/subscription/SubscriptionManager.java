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
package gg.essential.network.connectionmanager.subscription;

import com.google.common.collect.Sets;
import gg.essential.connectionmanager.common.packet.subscription.SubscriptionUpdatePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.queue.PacketQueue;
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue;
import gg.essential.util.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SubscriptionManager implements NetworkedManager {

    @NotNull
    private final PacketQueue packetQueue;

    private final List<Listener> listeners = new ArrayList<>();

    private final Set<UUID> subscriptions = Sets.newConcurrentHashSet();

    public SubscriptionManager(@NotNull ConnectionManager connectionManager) {
        this.packetQueue = new SequentialPacketQueue.Builder(connectionManager)
                .onTimeoutRetransmit()
                .create();
    }

    public boolean isSubscribedOrSelf(@NotNull UUID uuid) {
        return this.isSubscribed(uuid) || uuid.equals(UUIDUtil.getClientUUID());
    }

    public boolean isSubscribed(@NotNull UUID uuid) {
        return this.subscriptions.contains(uuid);
    }

    public void subscribeToFeeds(@NotNull Set<UUID> uuids) {
        uuids.remove(UUIDUtil.getClientUUID());

        this.subscriptions.addAll(uuids);
        this.packetQueue.enqueue(new SubscriptionUpdatePacket(uuids.toArray(new UUID[0]), true), response -> {
            for (Listener listener : this.listeners) {
                listener.onSubscriptionAdded(uuids);
            }
        });
    }

    public void unSubscribeFromFeeds(@NotNull Set<UUID> uuids) {
        uuids.remove(UUIDUtil.getClientUUID());

        this.subscriptions.removeAll(uuids);
        this.packetQueue.enqueue(new SubscriptionUpdatePacket(uuids.toArray(new UUID[0]), false), response -> {
            for (Listener listener : this.listeners) {
                listener.onSubscriptionRemoved(uuids);
            }
        });
    }

    @Override
    public void onConnected() {
        this.packetQueue.reset();

        // Clear all subscriptions, they are no longer valid as we may have missed updates
        Set<UUID> uuids = new HashSet<>(this.subscriptions);

        resetState();

        // Re-subscribe to all the UUIDs we are still interested in
        this.subscribeToFeeds(uuids);
    }

    @Override
    public void resetState() {
        Set<UUID> uuids = new HashSet<>(this.subscriptions);

        this.subscriptions.clear();

        for (Listener listener : this.listeners) {
            listener.onSubscriptionRemoved(uuids);
        }
    }

    public void addListener(@NotNull Listener listener) {
        this.listeners.add(listener);
    }

    public interface Listener {
        default void onSubscriptionAdded(@NotNull Set<UUID> uuids){}
        default void onSubscriptionRemoved(@NotNull Set<UUID> uuids){}
    }
}
