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
package gg.essential.network.connectionmanager;

import gg.essential.config.FeatureFlags;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.connection.*;
import gg.essential.connectionmanager.common.packet.multiplayer.ServerMultiplayerJoinServerPacket;
import gg.essential.connectionmanager.common.packet.relationships.ServerUuidNameMapPacket;
import gg.essential.event.client.PostInitializationEvent;
import gg.essential.network.client.MinecraftHook;
import gg.essential.network.connectionmanager.chat.ChatManager;
import gg.essential.network.connectionmanager.coins.CoinsManager;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.cosmetics.OutfitManager;
import gg.essential.network.connectionmanager.cosmetics.PacketHandlers;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.handler.connection.ClientConnectionDisconnectPacketHandler;
import gg.essential.network.connectionmanager.handler.connection.ServerConnectionReconnectPacketHandler;
import gg.essential.network.connectionmanager.handler.mojang.ServerUuidNameMapPacketHandler;
import gg.essential.network.connectionmanager.handler.multiplayer.ServerMultiplayerJoinServerPacketHandler;
import gg.essential.network.connectionmanager.ice.IIceManager;
import gg.essential.network.connectionmanager.ice.IceManager;
import gg.essential.network.connectionmanager.ice.IceManagerMcImpl;
import gg.essential.network.connectionmanager.media.ScreenshotManager;
import gg.essential.network.connectionmanager.notices.NoticesManager;
import gg.essential.network.connectionmanager.profile.ProfileManager;
import gg.essential.network.connectionmanager.relationship.RelationshipManager;
import gg.essential.network.connectionmanager.serverdiscovery.ServerDiscoveryManager;
import gg.essential.network.connectionmanager.skins.SkinsManager;
import gg.essential.network.connectionmanager.social.SocialManager;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.network.connectionmanager.subscription.SubscriptionManager;
import gg.essential.network.connectionmanager.telemetry.TelemetryManager;
import gg.essential.util.ModLoaderUtil;
import gg.essential.util.Multithreading;
import gg.essential.util.lwjgl3.Lwjgl3Loader;
import kotlin.Unit;
import kotlin.collections.MapsKt;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.JobKt;
import me.kbrewster.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static gg.essential.gui.elementa.state.v2.combinators.StateKt.map;
import static kotlinx.coroutines.ExceptionsKt.CancellationException;

public class ConnectionManager extends ConnectionManagerKt {

    @NotNull
    final PacketHandlers packetHandlers = new PacketHandlers();
    @NotNull
    private final MinecraftHook minecraftHook;
    @NotNull
    private final List<NetworkedManager> managers = new ArrayList<>();
    @NotNull
    private final NoticesManager noticesManager;
    @NotNull
    private final SubscriptionManager subscriptionManager;
    @NotNull
    private final RelationshipManager relationshipManager;
    @NotNull
    private final CosmeticsManager cosmeticsManager;
    @NotNull
    private final ChatManager chatManager;
    @NotNull
    private final ProfileManager profileManager;
    @NotNull
    private final SPSManager spsManager;
    @NotNull
    private final ServerDiscoveryManager serverDiscoveryManager;
    @NotNull
    private final SocialManager socialManager;
    @NotNull
    private final IIceManager iceManager;
    @NotNull
    private final ScreenshotManager screenshotManager;
    @NotNull
    private final TelemetryManager telemetryManager;
    //@NotNull
    private /*final*/ CoinsManager coinsManager;
    //@NotNull
    private /*final*/ SkinsManager skinsManager;
    @NotNull
    private final OutfitManager outfitManager;

    private boolean modsLoaded = false;
    private boolean modsSent = false;

    public enum Status {
        NO_TOS,
        ESSENTIAL_DISABLED,
        OUTDATED,
        CANCELLED,
        ALREADY_CONNECTED,
        NO_RESPONSE,
        INVALID_RESPONSE,
        MOJANG_UNAUTHORIZED,
        GENERAL_FAILURE,
        SUCCESS,
    }

    public ConnectionManager(@NotNull final MinecraftHook minecraftHook, File baseDir, Lwjgl3Loader lwjgl3) {
        this.minecraftHook = minecraftHook;
        this.subscriptionManager = new SubscriptionManager(this);
        this.managers.add(this.subscriptionManager);

        // Connections
        this.registerPacketHandler(ClientConnectionDisconnectPacket.class, new ClientConnectionDisconnectPacketHandler());
        this.registerPacketHandler(ServerConnectionReconnectPacket.class, new ServerConnectionReconnectPacketHandler());

        // Multiplayer
        this.registerPacketHandler(ServerMultiplayerJoinServerPacket.class, new ServerMultiplayerJoinServerPacketHandler());

        // Mojang API
        this.registerPacketHandler(ServerUuidNameMapPacket.class, new ServerUuidNameMapPacketHandler());

        // Notices
        this.managers.add((this.noticesManager = new NoticesManager(this)));

        // Cosmetics
        this.cosmeticsManager = new CosmeticsManager(this, baseDir);
        this.managers.add(this.cosmeticsManager);
        this.managers.add(this.cosmeticsManager.getEquippedCosmeticsManager());

        // Relationships
        this.relationshipManager = new RelationshipManager(this);
        this.managers.add(this.relationshipManager);

        // Chat
        this.chatManager = new ChatManager(this);
        this.managers.add(this.chatManager);

        // Profile
        this.profileManager = new ProfileManager(this);
        this.managers.add(this.profileManager);

        // SPS
        this.spsManager = new SPSManager(this);
        this.managers.add(this.spsManager);

        // Server Discovery
        this.serverDiscoveryManager = new ServerDiscoveryManager(this);
        this.managers.add(this.serverDiscoveryManager);

        // Social Manager
        this.managers.add(this.socialManager = new SocialManager(this));

        // Ice
        if (FeatureFlags.NEW_ICE_BACKEND_ENABLED) {
            this.iceManager = new IceManagerMcImpl(this, baseDir.toPath(), uuid -> this.spsManager.getInvitedUsers().contains(uuid));
        } else {
            IceManager iceManagerImpl = new IceManager(this, this.spsManager);
            this.managers.add(iceManagerImpl);
            this.iceManager = iceManagerImpl;
        }

        //Screenshots
        this.managers.add(this.screenshotManager = new ScreenshotManager(this, baseDir, lwjgl3));

        // Telemetry
        this.managers.add(this.telemetryManager = new TelemetryManager(this));

        // Coins
        this.managers.add(this.coinsManager = new CoinsManager(this));

        // Skins
        this.managers.add(this.skinsManager = new SkinsManager(this));

        // Outfits
        this.outfitManager = new OutfitManager(this, this.cosmeticsManager, map(this.skinsManager.getSkins(), map -> MapsKt.mapValues(map, it -> it.getValue().getSkin())));
        this.managers.add(this.outfitManager);

    }

    @NotNull
    public MinecraftHook getMinecraftHook() {
        return this.minecraftHook;
    }

    @NotNull
    public NoticesManager getNoticesManager() {
        return noticesManager;
    }

    @NotNull
    public SubscriptionManager getSubscriptionManager() {
        return this.subscriptionManager;
    }

    @NotNull
    public RelationshipManager getRelationshipManager() {
        return this.relationshipManager;
    }

    @NotNull
    public CosmeticsManager getCosmeticsManager() {
        return this.cosmeticsManager;
    }

    @NotNull
    public ChatManager getChatManager() {
        return this.chatManager;
    }

    @NotNull
    public ProfileManager getProfileManager() {
        return this.profileManager;
    }

    @NotNull
    public SPSManager getSpsManager() {
        return this.spsManager;
    }

    @NotNull
    public SocialManager getSocialManager() {
        return this.socialManager;
    }

    @NotNull
    public ScreenshotManager getScreenshotManager() {
        return screenshotManager;
    }

    @NotNull
    public IIceManager getIceManager() {
        return this.iceManager;
    }

    @NotNull
    public TelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    @NotNull
    public CoinsManager getCoinsManager() {
        return coinsManager;
    }

    @NotNull
    public SkinsManager getSkinsManager() {
        return skinsManager;
    }

    @NotNull
    public OutfitManager getOutfitManager() {
        return this.outfitManager;
    }

    public boolean isOpen() {
        Connection connection = this.connection;
        return connection != null && connection.isOpen();
    }

    public boolean isAuthenticated() {
        return this.connection != null;
    }

    public <T extends Packet> void registerPacketHandler(Class<T> cls, PacketHandler<T> handler) {
        this.packetHandlers.register(cls, handler);
    }

    @Override
    public <T extends Packet> void registerPacketHandler(@NotNull Class<T> cls, @NotNull Function1<? super T, Unit> handler) {
        registerPacketHandler(cls, new PacketHandler<T>() {
            @Override
            protected void onHandle(@NotNull ConnectionManager connectionManager, @NotNull T packet) {
                handler.invoke(packet);
            }
        });
    }

    protected void completeConnection(Connection connection) {
        this.connection = connection;

        for (NetworkedManager manager : this.managers) {
            manager.onConnected();
        }

        // Do not want to block the current thread for this (reads mod files to create checksums)
        if (modsLoaded && !modsSent) {
            Multithreading.runAsync(() -> {
                send(ModLoaderUtil.createModsAnnouncePacket());
            });
            modsSent = true;
        }
    }

    protected void onClose() {
        this.connection = null;
        this.modsSent = false;

        JobKt.cancelChildren(getConnectionScope().getCoroutineContext(), CancellationException("Connection closed.", null));

        for (NetworkedManager manager : this.managers) {
            manager.onDisconnect();
        }
    }

    /**
     * Send a packet to the Connection Manager not caring if it was truly received or a response for said packet.
     *
     * @param packet to send to the connection manager
     * @deprecated Use {@link #call(Packet)} builder instead.
     */
    @Deprecated
    public void send(@NotNull final Packet packet) {
        this.send(packet, null);
    }

    /**
     * Send a packet to the Connection Manager with a callback for when we get a response for the packet we sent
     * with a default time out of 10 seconds.
     *
     * @param packet           to send to the connection manager
     * @param responseCallback callback to use when we receive a response
     * @deprecated Use {@link #call(Packet)} builder instead.
     */
    @Deprecated
    public void send(
        @NotNull final Packet packet, @Nullable final Consumer<Optional<Packet>> responseCallback
    ) {
        this.send(packet, responseCallback, TimeUnit.SECONDS, 10L);
    }

    //

    /**
     * Send a packet to the Connection Manager with a callback for when we get a response for the packet we sent.
     * We also support timeouts for the packet we are sending so we can handle timeouts.
     *
     * @param packet           to send to the connection manager
     * @param responseCallback callback to use when we receive a response
     * @param timeoutUnit      time unit to use for the timeout
     * @param timeoutValue     value to use for the timeout time unit
     * @deprecated Use {@link #call(Packet)} builder instead.
     */
    @Deprecated
    @Override
    public void send(
        @NotNull final Packet packet, @Nullable final Consumer<Optional<Packet>> responseCallback,
        @Nullable final TimeUnit timeoutUnit, @Nullable final Long timeoutValue
    ) {
        Connection connection = this.connection;

        if (connection == null || !connection.isOpen()) {
            if (responseCallback != null) {
                responseCallback.accept(Optional.empty());
            }

            return;
        }

        final boolean wantsResponseHandling = (responseCallback != null && timeoutUnit != null && timeoutValue != null);
        UUID packetId = packet.getPacketUniqueId();
        packetId = (wantsResponseHandling && packetId == null ? UUID.randomUUID() : packetId);
        packet.setUniqueId(packetId);

        if (wantsResponseHandling) {
            this.packetHandlers.register(packetId, timeoutUnit, timeoutValue, responseCallback);
        }

        connection.send(packet);
    }

    @Subscribe
    public void onPostInit(PostInitializationEvent event) {
        modsLoaded = true;
        if (!modsSent && isAuthenticated()) {
            Multithreading.runAsync(() -> {
                send(ModLoaderUtil.createModsAnnouncePacket());
            });
            modsSent = true;
        }
    }

    public ServerDiscoveryManager getServerDiscoveryManager() {
        return this.serverDiscoveryManager;
    }

    public void onTosRevokedOrEssentialDisabled() {
        if (this.isOpen()) {
            this.close(CloseReason.USER_TOS_REVOKED);
        }
        for (NetworkedManager manager : this.managers) {
            manager.resetState();
        }
    }

}
