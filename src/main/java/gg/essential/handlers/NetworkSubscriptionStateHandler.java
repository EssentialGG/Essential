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
package gg.essential.handlers;

import gg.essential.Essential;
import gg.essential.data.OnboardingData;
import gg.essential.event.client.ClientTickEvent;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.subscription.SubscriptionManager;
import gg.essential.universal.UMinecraft;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;

//#if MC >= 11400
//$$ import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
//#endif

/**
 * Maintains user subscription to data from other users in its world
 */
public class NetworkSubscriptionStateHandler {

    private final Set<UUID> subscribedTo = new HashSet<>();

    @Subscribe
    public void tick(ClientTickEvent tickEvent) {
        if (!OnboardingData.hasAcceptedTos()) return;

        ConnectionManager connectionManager = Essential.getInstance().getConnectionManager();
        SubscriptionManager subscriptionManager = connectionManager.getSubscriptionManager();

        Set<UUID> currentTickList = new HashSet<>();
        WorldClient theWorld = UMinecraft.getWorld();
        if (theWorld != null) {
            //#if MC < 11400
            for (EntityPlayer playerEntity : theWorld.playerEntities) {
            //#else
            //$$ for (AbstractClientPlayerEntity playerEntity : theWorld.getPlayers()) {
            //#endif
                currentTickList.add(playerEntity.getUniqueID());
                if (playerEntity instanceof AbstractClientPlayerExt) {
                    currentTickList.add(((AbstractClientPlayerExt) playerEntity).getCosmeticsSourceUuid());
                }
            }
        }
        NetHandlerPlayClient netHandler = UMinecraft.getNetHandler();

        if (netHandler != null) {
            Collection<NetworkPlayerInfo> playerInfoMap = netHandler.getPlayerInfoMap();
            for (NetworkPlayerInfo info : playerInfoMap) {
                UUID id = info.getGameProfile().getId();
                currentTickList.add(id);
            }
        }
        Iterator<UUID> iterator = subscribedTo.iterator();
        Set<UUID> unsubFrom = new HashSet<>();
        while (iterator.hasNext()) {
            UUID next = iterator.next();
            if (!currentTickList.contains(next)) {
                unsubFrom.add(next);
                iterator.remove();
            }
        }
        Set<UUID> subTo = new HashSet<>();
        for (UUID uuid : currentTickList) {
            if (subscribedTo.add(uuid)) {
                if (uuid.version() != 4) {
                    continue;
                }
                subTo.add(uuid);
            }
        }

        if (subTo.size() > 0)
            subscriptionManager.subscribeToFeeds(subTo);
        if (unsubFrom.size() > 0)
            subscriptionManager.unSubscribeFromFeeds(unsubFrom);
    }
}
