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
package gg.essential.network.connectionmanager.notices;

import com.google.common.collect.Maps;
import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.notices.ClientNoticeBulkDismissPacket;
import gg.essential.connectionmanager.common.packet.notices.ClientNoticeRequestPacket;
import gg.essential.connectionmanager.common.packet.notices.ServerNoticeBulkDismissPacket;
import gg.essential.connectionmanager.common.packet.notices.ServerNoticePopulatePacket;
import gg.essential.connectionmanager.common.packet.notices.ServerNoticeRemovePacket;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.state.Sale;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.notices.handler.ServerNoticePopulatePacketHandler;
import gg.essential.network.connectionmanager.notices.handler.ServerNoticeRemovePacketHandler;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.notices.NoticeType;
import gg.essential.notices.model.Notice;
import gg.essential.util.Multithreading;
import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static gg.essential.gui.elementa.state.v2.StateKt.mutableStateOf;

public class NoticesManager implements NetworkedManager, INoticesManager {

    @NotNull
    private final Map<String, Notice> notices = Maps.newConcurrentMap();

    @NotNull
    private final Set<String> dismissNoticesQueue = new HashSet<>();

    @NotNull
    private final ConnectionManager connectionManager;

    @NotNull
    private final CosmeticNotices cosmeticNotices = new CosmeticNotices();

    @NotNull
    private final SaleNoticeManager saleNoticeManager = new SaleNoticeManager();

    @NotNull
    private final SocialMenuNewFriendRequestNoticeManager socialMenuNewFriendRequestNoticeManager;

    private NoticeBannerManager noticeBannerManager;

    private final List<NoticeListener> listeners = new ArrayList<>(Arrays.asList(cosmeticNotices, saleNoticeManager));

    public NoticesManager(@NotNull final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;

        connectionManager.registerPacketHandler(ServerNoticePopulatePacket.class, new ServerNoticePopulatePacketHandler(this));
        connectionManager.registerPacketHandler(ServerNoticeRemovePacket.class, new ServerNoticeRemovePacketHandler(this));

        listeners.add(new FriendRequestToastNoticeListener(connectionManager, this));
        listeners.add(new PersistentToastNoticeListener(this));

        noticeBannerManager = new NoticeBannerManager(this);
        listeners.add(noticeBannerManager);

        socialMenuNewFriendRequestNoticeManager = new SocialMenuNewFriendRequestNoticeManager(this);
        listeners.add(socialMenuNewFriendRequestNoticeManager);

        listeners.add(new GiftedCosmeticNoticeListener(this));

    }

    @NotNull
    public Map<String, Notice> getNotices() {
        return this.notices;
    }

    @NotNull
    public Optional<Notice> getNotice(@NotNull final String notice) {
        return Optional.ofNullable(this.notices.get(notice));
    }

    @NotNull
    public List<Notice> getNotices(
        @NotNull final NoticeType noticeType, @Nullable final Set<String> metadataKeys,
        @Nullable final Map<String, Object> metadataValues
    ) {
        return this.notices.values().stream()
            /* Notice Type */
            .filter(notice -> notice.getType() == noticeType)
            /* Metadata Keys */
            .filter(notice -> {
                if (metadataKeys == null || metadataKeys.isEmpty()) {
                    return true;
                }

                final Map<String, Object> metadata = notice.getMetadata();

                if (metadata.isEmpty()) {
                    return false;
                }

                for (@NotNull final String metadataKey : metadataKeys) {
                    if (!metadata.containsKey(metadataKey)) {
                        return false;
                    }
                }

                return true;
            })
            /* Metadata Values */
            .filter(notice -> {
                if (metadataValues == null || metadataValues.isEmpty()) {
                    return true;
                }

                final Map<String, Object> metadata = notice.getMetadata();

                if (metadata.isEmpty()) {
                    return false;
                }

                for (@NotNull final Map.Entry<String, Object> entry : metadataValues.entrySet()) {
                    final Object metadataValue = metadata.get(entry.getKey());

                    if (metadataValue == null) {
                        return false;
                    }

                    return Objects.equals(metadataValue, entry.getValue());
                }

                return false;
            })
            /* Collect */
            .collect(Collectors.toList());
    }

    @Override
    public void populateNotices(@NotNull final Collection<? extends Notice> notices) {
        for (@NotNull final Notice notice : notices) {
            this.notices.put(notice.getId(), notice);
            for (NoticeListener listener : listeners) {
                listener.noticeAdded(notice);
            }
        }
    }

    @Override
    public void removeNotices(@Nullable final Set<String> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            for (Notice value : notices.values()) {
                for (NoticeListener listener : listeners) {
                    listener.noticeRemoved(value);
                }
            }
            this.notices.clear();
            return;
        }

        for (@NotNull final String noticeId : noticeIds) {
            final Notice removed = this.notices.remove(noticeId);
            if (removed == null) {
                continue;
            }
            for (NoticeListener listener : listeners) {
                listener.noticeRemoved(removed);
            }
        }
    }

    @Override
    public void dismissNotice(String noticeId) {
        dismissNotices(SetsKt.setOf(noticeId));
    }

    public void dismissNotices(Set<String> noticeIds) {
        dismissNoticesQueue.addAll(noticeIds);
        flushDismissNotices();
    }

    public void flushDismissNotices() {
        if (dismissNoticesQueue.isEmpty()) {
            return;
        }
        final Set<String> notices = new HashSet<>(dismissNoticesQueue);
        this.dismissNoticesQueue.clear();
        this.connectionManager.send(new ClientNoticeBulkDismissPacket(notices), maybePacket -> {
            if (maybePacket.isPresent()) {
                Packet packet = maybePacket.get();
                if (packet instanceof ServerNoticeBulkDismissPacket) {
                    ServerNoticeBulkDismissPacket serverNoticeBulkDismissPacket = (ServerNoticeBulkDismissPacket) packet;
                    for (String noticeId : serverNoticeBulkDismissPacket.getNoticeIds()) {
                        this.notices.remove(noticeId);
                    }
                    for (ServerNoticeBulkDismissPacket.ErrorDetails error : serverNoticeBulkDismissPacket.getErrors()) {
                        switch (error.getReason()) {
                            case "NOTICE_NOT_FOUND":
                            case "NOTICE_ALREADY_DISMISSED": {
                                this.notices.remove(error.getNoticeId());
                                break;
                            }
                            default: {
                                Essential.logger.error("Notice unable to be dismissed: NoticeId: {}, Reason: {}", error.getNoticeId(), error.getReason());
                                break;
                            }
                        }
                    }
                    return;
                }
            }
            Essential.logger.error("Unexpected notice response: {}", maybePacket);
        });
    }

    public SocialMenuNewFriendRequestNoticeManager getSocialMenuNewFriendRequestNoticeManager() {
        return socialMenuNewFriendRequestNoticeManager;
    }

    public @NotNull NoticeBannerManager getNoticeBannerManager() {
        return noticeBannerManager;
    }

    @Override
    public void resetState() {
        this.notices.clear();

        listeners.forEach(NoticeListener::resetState);
    }

    @Override
    public void onConnected() {
        resetState();

        connectionManager.send(new ClientNoticeRequestPacket(null, SetsKt.setOf(NoticeType.values()), null, null));
        listeners.forEach(NoticeListener::onConnect);
    }

    public @NotNull CosmeticNotices getCosmeticNotices() {
        return cosmeticNotices;
    }

    public @NotNull SaleNoticeManager getSaleNoticeManager() {
        return saleNoticeManager;
    }


    public class CosmeticNotices implements NoticeListener {

        private final String METADATA_KEY = "cosmetic_id";
        private final ConcurrentHashMap<String, MutableState<Boolean>> cosmeticToNewStateMap = new ConcurrentHashMap<>();
        private final MutableState<Boolean> hasAnyNewCosmetics = mutableStateOf(false);

        public State<Boolean> getNewState(String cosmeticId) {
            return cosmeticToNewStateMap.computeIfAbsent(cosmeticId, ignored -> mutableStateOf(false));
        }

        private void updateGlobalState() {
            hasAnyNewCosmetics.set(
                cosmeticToNewStateMap.entrySet().stream()
                    .anyMatch(entry -> {
                        if (!entry.getValue().get()) {
                            return false;
                        }
                        final Cosmetic cosmetic = connectionManager.getCosmeticsManager().getCosmetic(entry.getKey());
                        if (cosmetic == null) {
                            return false;
                        }

                        return cosmetic.isAvailable();
                    })
            );
        }

        public State<Boolean> getHasAnyNewCosmetics() {
            return hasAnyNewCosmetics;
        }

        @Override
        public void noticeAdded(Notice notice) {
            if (notice.getType() == NoticeType.NEW_BANNER && notice.getMetadata().containsKey(METADATA_KEY)) {
                String cosmeticId = (String) notice.getMetadata().get(METADATA_KEY);
                cosmeticToNewStateMap.computeIfAbsent(cosmeticId, ignored -> mutableStateOf(true)).set(true);

                updateGlobalState();
            }
        }

        @Override
        public void noticeRemoved(Notice notice) {
            // No impl for this manager
        }

        @Override
        public void onConnect() {
            resetState();
        }

        public void cosmeticAdded(String id) {
            final MutableState<Boolean> existingState = cosmeticToNewStateMap.get(id);
            if (existingState != null && existingState.get()) {
                updateGlobalState();
            }

        }
    }

    public class SaleNoticeManager implements NoticeListener {

        private final boolean saleSuppressedByJvmFlag = System.getProperty("essential.disableSale", "false").equals("true");
        private final MutableState<@NotNull Set<Sale>> currentState = mutableStateOf(Collections.emptySet());

        private final Map<String, Sale> salesMap = new HashMap<>();
        private ScheduledFuture<?> nextUpdateFuture = null;

        @Override
        public void noticeAdded(Notice notice) {
            if (saleSuppressedByJvmFlag) {
                return;
            }

            if (notice.getType() == NoticeType.SALE) {
                if (notice.getExpiresAt() == null) {
                    Essential.logger.error("Notice " + notice.getId() + " is type sale but does not have an expiration date set!");
                    return;
                }
                final int discount = ((Number) notice.getMetadata().get("discount")).intValue();
                @Nullable Set<Integer> packagesSet = null;
                if (notice.getMetadata().containsKey("packages")) {
                    packagesSet = new HashSet<>();
                    for (Number packages : (Collection<Number>) notice.getMetadata().get("packages")) {
                        packagesSet.add(packages.intValue());
                    }
                    if (packagesSet.isEmpty()) {
                        packagesSet = null;
                    }
                }
                Set<String> onlyCosmetics = null;
                if (notice.getMetadata().containsKey("cosmetics")) {
                    onlyCosmetics = new HashSet<>((Collection<String>) notice.getMetadata().get("cosmetics"));
                }
                salesMap.put(
                    notice.getId(),
                    new Sale(
                        notice.getExpiresAt().toInstant(),
                        ((String) notice.getMetadata().get("sale_name")),
                        notice.getMetadata().containsKey("sale_name_compact") ? ((String) notice.getMetadata().get("sale_name_compact")) : (discount == 0 ? null : "SALE"),
                        discount,
                        (Boolean) notice.getMetadata().getOrDefault("display_time", Boolean.TRUE),
                        (String) notice.getMetadata().get("category"),
                        packagesSet,
                        onlyCosmetics,
                        (String) notice.getMetadata().get("tooltip"),
                        (String) notice.getMetadata().get("coupon")
                    )
                );
                refreshState();
            }
        }

        private void refreshState() {
            currentState.set(new HashSet<>(salesMap.values()));
            scheduleUpdate();
        }

        private void scheduleUpdate() {
            if (nextUpdateFuture != null) {
                nextUpdateFuture.cancel(false);
            }
            Optional<Sale> first = salesMap.values().stream().min(Comparator.comparing(Sale::getExpiration));
            if (first.isPresent()) {
                final Sale sale = first.get();
                nextUpdateFuture = Multithreading.scheduleOnMainThread(() -> {
                    final Instant now = Instant.now();

                    if (salesMap.entrySet().removeIf(entry -> entry.getValue().getExpiration().isBefore(now))) {
                        refreshState();
                    }
                }, Instant.now().until(sale.getExpiration(), ChronoUnit.MILLIS) + 1000, TimeUnit.MILLISECONDS);
            }
        }

        public State<@NotNull Set<Sale>> getSaleState() {
            return currentState;
        }

        @Override
        public void noticeRemoved(Notice notice) {
            if (notice.getType() == NoticeType.SALE) {
                salesMap.remove(notice.getId());
                refreshState();
            }
        }

        @Override
        public void onConnect() {}
    }


}
