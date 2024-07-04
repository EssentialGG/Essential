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
package gg.essential.network.connectionmanager.cosmetics;

import com.google.common.collect.Maps;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.caffeine.cache.Cache;
import gg.essential.lib.caffeine.cache.Caffeine;
import gg.essential.lib.caffeine.cache.Expiry;
import gg.essential.lib.caffeine.cache.RemovalCause;
import gg.essential.lib.caffeine.cache.Scheduler;
import gg.essential.network.connectionmanager.AsyncResponseHandler;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.EarlyResponseHandler;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.universal.UMinecraft;
import gg.essential.util.ExtensionsKt;
import gg.essential.util.Multithreading;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PacketHandlers {
    @NotNull
    private final Executor mainThreadExecutor = ExtensionsKt.getExecutor(UMinecraft.getMinecraft());

    @NotNull
    private final Map<Class<? extends Packet>, PacketHandler<?>> packetHandlers = Maps.newHashMap();

    @NotNull
    private final Cache<@NotNull UUID, @NotNull Pair<@NotNull Long, @NotNull Consumer<@NotNull Optional<Packet>>>> awaitingPacketResponses =
        Caffeine.newBuilder()
            .maximumSize(10000)
            .executor(Multithreading.getPool())
            .scheduler(Scheduler.forScheduledExecutorService(Multithreading.getScheduledPool()))
            .expireAfter(new Expiry<UUID, Pair<@NotNull Long, @NotNull Consumer<@NotNull Optional<Packet>>>>() {

                @Override
                public long expireAfterCreate(@NotNull final UUID packetId, @NotNull final Pair<@NotNull Long, @NotNull Consumer<@NotNull Optional<Packet>>> valueData, final long currentTime) {
                    return valueData.getKey();
                }

                @Override
                public long expireAfterUpdate(@NotNull final UUID packetId, @NotNull final Pair<@NotNull Long, @NotNull Consumer<@NotNull Optional<Packet>>> valueData, final long currentTime, final long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(@NotNull final UUID packetId, @NotNull final Pair<@NotNull Long, @NotNull Consumer<@NotNull Optional<Packet>>> valueData, final long currentTime, final long currentDuration) {
                    return currentDuration;
                }

            })
            .evictionListener((key, value, cause) -> {
                if (value != null && (RemovalCause.EXPIRED == cause || RemovalCause.SIZE == cause)) {
                    Consumer<@NotNull Optional<Packet>> packetHandler = value.getRight();
                    this.mainThreadExecutor.execute(() -> packetHandler.accept(Optional.empty()));
                }
            })
            .build();

    public <T extends Packet> void register(Class<T> cls, PacketHandler<T> handler) {
        this.packetHandlers.put(cls, handler);
    }

    public void register(
        @NotNull final UUID packetId,
        @NotNull final TimeUnit timeoutUnit,
        @NotNull final Long timeoutValue,
        @NotNull final Consumer<Optional<Packet>> responseCallback
    ) {
        this.awaitingPacketResponses.put(packetId, Pair.of(timeoutUnit.toNanos(timeoutValue), responseCallback));
    }

    public void handle(ConnectionManager connectionManager, Packet packet) {
        final Consumer<Optional<Packet>> fResponseHandler;
        final Consumer<Optional<Packet>> asyncResponseHandler;
        final PacketHandler packetHandler = this.packetHandlers.get(packet.getClass());

        // Retrieve the packetId and responseHandler for the incoming packet if need be.
        UUID packetId = packet.getPacketUniqueId();
        Consumer<Optional<Packet>> responseHandler = null;
        if (packetId != null) {
            final Pair<@NotNull Long, @NotNull Consumer<@NotNull Optional<Packet>>> responseCallbackPair = this.awaitingPacketResponses.getIfPresent(packetId);

            if (responseCallbackPair != null) {
                this.awaitingPacketResponses.invalidate(packetId);
                responseHandler = responseCallbackPair.getRight();
            }
        }
        if (responseHandler instanceof AsyncResponseHandler) {
            fResponseHandler = null;
            asyncResponseHandler = responseHandler;
        } else {
            fResponseHandler = responseHandler;
            asyncResponseHandler = null;
        }

        if (packetHandler == null && responseHandler == null) {
            return;
        }

        Runnable syncPacketHandler = null;
        if (packetHandler != null) {
            try {
                syncPacketHandler = packetHandler.handleAsync(connectionManager, packet);
            } catch (final Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        Runnable fSyncPacketHandler = syncPacketHandler;

        if (asyncResponseHandler != null) {
            try {
                asyncResponseHandler.accept(Optional.of(packet));
            } catch (final Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        this.mainThreadExecutor.execute(() -> {
            // If the connection closed since we went from read thread -> main thread then throw out the packet.
            if (!connectionManager.isOpen()) {
                return;
            }

            Consumer<Optional<Packet>> responseHandlerSync = fResponseHandler;

            if (responseHandlerSync instanceof EarlyResponseHandler) {
                try {
                    responseHandlerSync.accept(Optional.of(packet));
                } catch (final Throwable throwable) {
                    throwable.printStackTrace();
                }
                responseHandlerSync = null;
            }

            if (fSyncPacketHandler != null) {
                try {
                    fSyncPacketHandler.run();
                } catch (final Throwable throwable) {
                    throwable.printStackTrace();
                }
            }

            if (responseHandlerSync != null) {
                try {
                    responseHandlerSync.accept(Optional.of(packet));
                } catch (final Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
    }
}
