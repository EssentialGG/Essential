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
import gg.essential.event.sps.SPSStartEvent;
import gg.essential.mixins.ext.client.multiplayer.ServerDataExt;
import gg.essential.util.AddressUtil;
import me.kbrewster.eventbus.Subscribe;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.enums.ActivityType;
import gg.essential.data.OnboardingData;
import gg.essential.event.gui.GuiOpenEvent;
import gg.essential.event.network.server.ServerJoinEvent;
import gg.essential.event.network.server.ServerLeaveEvent;
import gg.essential.event.network.server.SingleplayerJoinEvent;
import net.minecraft.client.gui.GuiMainMenu;
import gg.essential.network.connectionmanager.ConnectionManager;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.Nullable;

import static gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt.getExt;

/**
 * Updates user status on Essential network
 */
public class ServerStatusHandler {

    @Subscribe
    public void onGuiSwitch(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiMainMenu)
            setActivity(null, null);
    }


    @Subscribe
    public void disconnectEvent(ServerLeaveEvent event) {
        setActivity(null, null);
    }

    private void setActivity(@Nullable final ActivityType activityType, @Nullable final String metadata) {
        if (!OnboardingData.hasAcceptedTos() || !EssentialConfig.INSTANCE.getEssentialFull()) return;
        ConnectionManager connectionManager = Essential.getInstance().getConnectionManager();
        connectionManager.getProfileManager().updatePlayerActivity(activityType, metadata);
    }

    @Subscribe
    public void connect(ServerJoinEvent event) {
        ServerData serverData = event.getServerData();
        ServerDataExt serverDataExt = getExt(serverData);

        Boolean shareWithFriends = serverDataExt.getEssential$shareWithFriends();
        if (shareWithFriends == null) {
            shareWithFriends = EssentialConfig.INSTANCE.getSendServerUpdates();
        }
        if (!shareWithFriends) return;

        String serverIP = serverData.serverIP;
        if (AddressUtil.isLanOrLocalAddress(serverIP)) {
            setActivity(ActivityType.PLAYING, AddressUtil.LOCAL_SERVER);
        } else {
            setActivity(ActivityType.PLAYING, AddressUtil.removeDefaultPort(serverIP));
        }
    }

    @Subscribe
    public void joinSinglePlayer(SingleplayerJoinEvent event) {
        setActivity(ActivityType.PLAYING, AddressUtil.SINGLEPLAYER);
    }

    @Subscribe
    public void hostWorld(SPSStartEvent event) {
        if (!EssentialConfig.INSTANCE.getSendServerUpdates()) return;
        setActivity(ActivityType.PLAYING, event.getAddress());
    }

}
