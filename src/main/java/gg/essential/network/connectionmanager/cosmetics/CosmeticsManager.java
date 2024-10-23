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

import com.google.common.collect.ImmutableMap;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutCosmeticsPacket;
import gg.essential.connectionmanager.common.packet.cosmetic.*;
import gg.essential.connectionmanager.common.packet.cosmetic.categories.ServerCosmeticCategoriesPopulatePacket;
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket;
import gg.essential.cosmetics.EquippedCosmetic;
import gg.essential.cosmetics.model.CosmeticUnlockData;
import gg.essential.connectionmanager.common.packet.wardrobe.ClientWardrobeSettingsPacket;
import gg.essential.connectionmanager.common.packet.wardrobe.ServerWardrobeSettingsPacket;
import gg.essential.connectionmanager.common.packet.wardrobe.ServerWardrobeStoreBundlePacket;
import gg.essential.data.OnboardingData;
import gg.essential.elementa.state.v2.ReferenceHolder;
import gg.essential.event.client.ClientTickEvent;
import gg.essential.event.network.server.ServerJoinEvent;
import gg.essential.event.sps.PlayerJoinSessionEvent;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.elementa.state.v2.StateKt;
import gg.essential.gui.elementa.state.v2.collections.TrackedList;
import gg.essential.gui.modals.TOSModal;
import gg.essential.gui.notification.Notifications;
import gg.essential.mod.EssentialAsset;
import gg.essential.mod.cosmetics.*;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.handler.cosmetics.*;
import gg.essential.network.connectionmanager.handler.wardrobe.ServerWardrobeStoreBundlePacketHandler;
import gg.essential.network.connectionmanager.queue.PacketQueue;
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.network.cosmetics.cape.CapeCosmeticsManager;
import gg.essential.universal.UMinecraft;
import gg.essential.util.GuiUtil;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.MapsKt;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static gg.essential.gui.elementa.state.v2.combinators.StateKt.map;
import static gg.essential.gui.notification.HelpersKt.sendTosNotification;
import static gg.essential.network.connectionmanager.cosmetics.ConnectionManagerKt.*;

public class CosmeticsManager implements NetworkedManager {

    public static final long LOAD_TIMEOUT_SECONDS = 60;

    @NotNull
    private final ConnectionManager connectionManager;

    @SuppressWarnings("FieldCanBeLocal")
    private final ReferenceHolder refHolder = new ReferenceHolderImpl();

    @NotNull
    private final PacketQueue updateQueue;
    @NotNull
    private final CapeCosmeticsManager capeManager;
    @NotNull
    private final WardrobeSettings wardrobeSettings;
    @NotNull
    private final InfraCosmeticsData infraCosmeticsData;
    @Nullable
    private final LocalCosmeticsData localCosmeticsData;
    @NotNull
    private final CosmeticsData cosmeticsData;
    @Nullable
    private final CosmeticsDataWithChanges cosmeticsDataWithChanges;
    @NotNull
    private final EquippedCosmeticsManager equippedCosmeticsManager;
    @NotNull
    private final MutableState<Map<String, CosmeticUnlockData>> unlockedCosmeticsData = StateKt.mutableStateOf(new HashMap<>());
    @NotNull
    private final State<Set<String>> unlockedCosmetics = map(unlockedCosmeticsData, Map::keySet);
    @NotNull
    private final AssetLoader assetLoader;
    @NotNull
    private final ModelLoader modelLoader;

    @NotNull
    private CompletableFuture<Void> cosmeticsLoadedFuture = new CompletableFuture<>();
    private boolean receivedUnlockPacket = false;

    // If we've warned the user about cosmetics not working on offline mode servers
    private boolean shownOfflineModeWarning = false;

    public CosmeticsManager(@NotNull final ConnectionManager connectionManager, File baseDir) {
        this.connectionManager = connectionManager;

        this.assetLoader = new AssetLoader(baseDir.toPath().resolve("cosmetic-cache"));
        this.modelLoader = new ModelLoader(assetLoader);

        this.updateQueue = new SequentialPacketQueue.Builder(connectionManager)
                .onTimeoutRetransmit()
                .create();

        this.capeManager = new CapeCosmeticsManager(connectionManager, this);

        this.wardrobeSettings = new WardrobeSettings();

        this.infraCosmeticsData = new InfraCosmeticsData(connectionManager, assetLoader);

        Path repoPath = baseDir.toPath().resolve("cosmetics");
        if (Files.exists(repoPath)) {
            this.localCosmeticsData = new LocalCosmeticsData(repoPath, assetLoader);
            this.cosmeticsDataWithChanges = new CosmeticsDataWithChanges(localCosmeticsData);
            this.cosmeticsData = cosmeticsDataWithChanges;
        } else {
            this.localCosmeticsData = null;
            this.cosmeticsData = this.infraCosmeticsData;
            this.cosmeticsDataWithChanges = null;
        }

        this.equippedCosmeticsManager = new EquippedCosmeticsManager(this.connectionManager, this.capeManager, this.cosmeticsData, this.infraCosmeticsData);

        onNewCosmetic(this.cosmeticsData, refHolder, cosmetic -> {
            primeCache(modelLoader, assetLoader, cosmetic);

            connectionManager.getNoticesManager().getCosmeticNotices().cosmeticAdded(cosmetic.getId());

            // If we're side-loading, auto-unlock all cosmetics
            if (localCosmeticsData != null) {
                unlockedCosmeticsData.set(set ->
                    MapsKt.plus(set, new Pair<>(cosmetic.getId(), new CosmeticUnlockData(new DateTime(), null, true)))
                );
            }
        });

        connectionManager.registerPacketHandler(ServerCosmeticsPopulatePacket.class, new ServerCosmeticsPopulatePacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticTypesPopulatePacket.class, new ServerCosmeticTypesPopulatePacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticsUserUnlockedPacket.class, new ServerCosmeticsUserUnlockedPacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticsRevokePurchasePacket.class, new ServerCosmeticsRevokePurchasePacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticAnimationTriggerPacket.class, new ServerCosmeticAnimationTriggerPacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticsUserEquippedVisibilityPacket.class, new ServerCosmeticsUserEquippedVisibilityPacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticsSkinTexturePacket.class, new ServerCosmeticSkinTexturePacketHandler());
        connectionManager.registerPacketHandler(ServerCosmeticCategoriesPopulatePacket.class, new ServerCosmeticCategoriesPopulatePacketHandler(this));
        connectionManager.registerPacketHandler(ServerWardrobeSettingsPacket.class, new ServerWardrobeSettingsPacketHandler());
        connectionManager.registerPacketHandler(ServerWardrobeStoreBundlePacket.class, new ServerWardrobeStoreBundlePacketHandler());

        Essential.EVENT_BUS.register(this);
    }

    @NotNull
    public AssetLoader getAssetLoader() {
        return this.assetLoader;
    }

    @NotNull
    public ModelLoader getModelLoader() {
        return this.modelLoader;
    }

    @NotNull
    public CapeCosmeticsManager getCapeManager() {
        return this.capeManager;
    }

    @NotNull
    public EquippedCosmeticsManager getEquippedCosmeticsManager() {
        return this.equippedCosmeticsManager;
    }

    @NotNull
    public State<TrackedList<Cosmetic>> getCosmetics() {
        return cosmeticsData.getCosmetics();
    }

    public boolean getOwnCosmeticsVisible() {
        return this.equippedCosmeticsManager.getOwnCosmeticsVisible();
    }

    public void setOwnCosmeticsVisible(final boolean state) {
        this.equippedCosmeticsManager.setOwnCosmeticsVisible(state);
    }

    @NotNull
    public State<Set<String>> getUnlockedCosmetics() {
        return this.unlockedCosmetics;
    }

    public State<Map<String, CosmeticUnlockData>> getUnlockedCosmeticsData() {
        return this.unlockedCosmeticsData;
    }

    public void addUnlockedCosmeticsData(@NotNull final Map<String, CosmeticUnlockData> unlockedCosmeticsData) {
        this.infraCosmeticsData.requestCosmeticsIfMissing(unlockedCosmeticsData.keySet());
        this.unlockedCosmeticsData.set(set -> MapsKt.plus(set, unlockedCosmeticsData));
        this.receivedUnlockPacket = true;
    }

    public void unlockAllCosmetics() {
        CosmeticUnlockData unlockData = new CosmeticUnlockData(new DateTime(), null, true);
        this.unlockedCosmeticsData.set(set -> cosmeticsData.getCosmetics().get().stream().map(Cosmetic::getId)
            .collect(Collectors.toMap(k -> k, v -> unlockData)));
    }

    @NotNull
    public Map<CosmeticSlot, String> getEquippedCosmetics() {
        return this.equippedCosmeticsManager.getEquippedCosmetics();
    }

    public void updateEquippedCosmetic(@NotNull CosmeticSlot slot, @Nullable String cosmeticId) {
        CosmeticOutfit selectedOutfit = this.connectionManager.getOutfitManager().getSelectedOutfit();
        if (selectedOutfit != null) {
            updateEquippedCosmetic(selectedOutfit.getId(), slot, cosmeticId);
        }
    }

    public void updateEquippedCosmetic(@NotNull CosmeticOutfit outfit, @NotNull CosmeticSlot slot, @Nullable String cosmeticId) {
        updateEquippedCosmetic(outfit.getId(), slot, cosmeticId);
    }

    public void updateEquippedCosmetic(String outfitId, @NotNull CosmeticSlot slot, @Nullable String cosmeticId) {
        this.connectionManager.getOutfitManager().updateEquippedCosmetic(outfitId, slot, cosmeticId);
    }

    @NotNull
    public ImmutableMap<CosmeticSlot, EquippedCosmetic> getVisibleCosmetics(UUID playerId) {
        return this.equippedCosmeticsManager.getVisibleCosmetics(playerId);
    }

    @Nullable
    public Cosmetic getCosmetic(@NotNull final String cosmeticId) {
        return this.cosmeticsData.getCosmetic(cosmeticId);
    }

    public @NotNull CompletableFuture<byte[]> getAssetBytes(@NotNull EssentialAsset asset, @NotNull AssetLoader.Priority priority) {
        return assetLoader.getAssetBytes(asset, priority);
    }

    public void clearUnlockedCosmetics(boolean allowAutoUnlockIfSideloading) {
        unlockedCosmeticsData.set(set -> new HashMap<>());

        // If we're side-loading, auto-unlock all cosmetics
        if (allowAutoUnlockIfSideloading && localCosmeticsData != null) {
            unlockAllCosmetics();
        }
    }

    @Override
    public void resetState() {
        this.updateQueue.reset();
        this.clearUnlockedCosmetics(true);
        this.infraCosmeticsData.resetState();
        this.cosmeticsLoadedFuture = new CompletableFuture<>();
        this.receivedUnlockPacket = false;
        connectionManager.send(new ClientWardrobeSettingsPacket());
    }

    @Override
    public void onConnected() {
        resetState();
    }

    @Subscribe
    public void onSpsJoin(PlayerJoinSessionEvent event) {
        // Unlock SPS cosmetics if players join an SPS session that the user is hosting
        unlockSpsCosmetics(connectionManager);
    }

    @Subscribe
    public void onWorldJoin(ServerJoinEvent event) {
        if (!connectionManager.isAuthenticated()) return;

        // Show a notification about cosmetics not working on offline-mode servers on the first join of an offline-mode server.
        if (!EssentialConfig.INSTANCE.getDisableCosmetics() && !shownOfflineModeWarning) {
            NetHandlerPlayClient handler = UMinecraft.getMinecraft().getConnection();
            if (handler != null && !handler.getNetworkManager().isEncrypted() && !Minecraft.getMinecraft().isIntegratedServerRunning()) {
                gg.essential.gui.notification.ExtensionsKt.warning(
                    Notifications.INSTANCE,
                    "Wardrobe items unavailable",
                    "Wardrobe items are not visible on offline-mode servers."
                );

                shownOfflineModeWarning = true;
            }
        }

        // Unlock SPS cosmetics if user is joining, not hosting, an SPS session
        SPSManager spsManager = connectionManager.getSpsManager();
        if (spsManager.isSpsAddress(event.getServerData().serverIP) && spsManager.getLocalSession() == null) {
            unlockSpsCosmetics(connectionManager);
        }

        // Get any cosmetics that should be unlocked for joining the server
        unlockServerCosmetics(connectionManager, event.getServerData().serverIP);
    }

    /**
     * Toggles the users cosmetic visibility state or print error if not connected to the Connection Manager
     */
    public void toggleOwnCosmeticVisibility(boolean notification) {
        final boolean nextState = !getOwnCosmeticsVisible();
        setOwnCosmeticVisibility(notification, nextState);
    }

    /**
     * Sets the users cosmetic visibility state or print error if not connected to the Connection Manager
     */
    public void setOwnCosmeticVisibility(boolean notification, final boolean visible) {
        if (!connectionManager.isAuthenticated()) {
            if (OnboardingData.hasAcceptedTos()) {
                gg.essential.gui.notification.ExtensionsKt.error(
                    Notifications.INSTANCE,
                    "Essential Network Error", "Unable to establish connection with the Essential Network.",
                    () -> Unit.INSTANCE, () -> Unit.INSTANCE, b -> Unit.INSTANCE
                );
            } else {
                if (GuiUtil.INSTANCE.openedScreen() == null) {
                    // Show a notification when we're not in any menu, so it's less intrusive
                    sendTosNotification(() -> {
                        GuiUtil.INSTANCE.pushModal(
                            (manager) -> new TOSModal(manager, false, true, (it) -> Unit.INSTANCE, () -> Unit.INSTANCE)
                        );
                        return Unit.INSTANCE;
                    });
                } else {
                    GuiUtil.INSTANCE.pushModal(
                        (manager) -> new TOSModal(manager, false, true, (it) -> Unit.INSTANCE, () -> Unit.INSTANCE)
                    );
                }
            }
            return;
        }

        if (visible != getOwnCosmeticsVisible()) {
            connectionManager.send(
                new ClientCosmeticsUserEquippedVisibilityTogglePacket(visible),
                new CosmeticEquipVisibilityResponse(visible, notification)
            );
        }
    }

    public CompletableFuture<Boolean> claimFreeItems(@NotNull Set<String> ids) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        connectionManager.send(new ClientCheckoutCosmeticsPacket(ids, null), it -> {
            if (it.isPresent()) {
                Packet packet = it.get();
                if (packet instanceof ResponseActionPacket) {
                    // ResponseActionPacket(true) means that no error occurred, but no cosmetics were unlocked
                    if (!((ResponseActionPacket) packet).isSuccessful()) {
                        Essential.debug.error("ClientCosmeticCheckoutPacket did give an expected response");
                    }
                } else if (packet instanceof ServerCosmeticsUserUnlockedPacket) {
                    future.complete(true);
                    ServerCosmeticsUserUnlockedPacket unlockedPacket = (ServerCosmeticsUserUnlockedPacket) packet;
                    Essential.debug.debug(String.format("Automatically unlocked %d free cosmetics.", unlockedPacket.getUnlockedCosmetics().size()));
                }
            } else {
                Essential.debug.error("ClientCosmeticCheckoutPacket did not give a response");
            }

            if (!future.isDone()) {
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * Returns true if the client has received cosmetics, categories, and types from infra, and is not waiting for any further responses (or requests have timed out).
     */
    private boolean cosmeticDataLoadedFromInfra() {
        long timeout = LOAD_TIMEOUT_SECONDS * 1000;
        return (wardrobeSettings.isSettingsLoaded())
                && receivedUnlockPacket
                && !cosmeticsData.getCosmetics().get().isEmpty()
                && !cosmeticsData.getCategories().get().isEmpty()
                && !cosmeticsData.getTypes().get().isEmpty()
                && (cosmeticsData != infraCosmeticsData || !infraCosmeticsData.hasActiveRequests(timeout));
    }

    /**
     * Returns a future completed when client has received all cosmetic data from infra.
     */
    public @NotNull CompletableFuture<Void> getCosmeticsLoadedFuture() {
        return cosmeticsLoadedFuture;
    }

    @Subscribe
    public void tick(ClientTickEvent tickEvent) {
        if (!cosmeticsLoadedFuture.isDone() && cosmeticDataLoadedFromInfra()) {
            cosmeticsLoadedFuture.complete(null);
        }
    }

    /**
     * Notify the mod that the contained ids are no longer unlocked by the user. The Connection Manager
     * will send updated variants of the users outfits if one or more contained revoked cosmetics.
     *
     * @param revokedIds IDs of cosmetics no longer unlocked
     */
    public void removeUnlockedCosmetics(List<String> revokedIds) {
        this.unlockedCosmeticsData.set(set -> set.entrySet().stream()
                .filter(e -> !revokedIds.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @NotNull
    public WardrobeSettings getWardrobeSettings() {
        return wardrobeSettings;
    }

    @NotNull
    public CosmeticsData getCosmeticsData() {
        return cosmeticsData;
    }

    @NotNull
    public InfraCosmeticsData getInfraCosmeticsData() {
        return infraCosmeticsData;
    }

    @Nullable
    public LocalCosmeticsData getLocalCosmeticsData() {
        return localCosmeticsData;
    }

    @Nullable
    public CosmeticsDataWithChanges getCosmeticsDataWithChanges() {
        return cosmeticsDataWithChanges;
    }
}
