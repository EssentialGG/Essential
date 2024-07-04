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
package gg.essential.network.connectionmanager.social.handler;

import gg.essential.connectionmanager.common.packet.social.ClientSocialDiscordRequestJoinServerResponsePacket;
import gg.essential.connectionmanager.common.packet.social.SocialDiscordRequestJoinServerPacket;
import gg.essential.handlers.discord.DiscordIntegration;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.sps.SPSManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;

public class SocialDiscordRequestJoinServerPacketHandler extends PacketHandler<SocialDiscordRequestJoinServerPacket> {
    @Override
    protected void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final SocialDiscordRequestJoinServerPacket packet) {
        final UUID target = packet.getTargetUUID();
        final String address = DiscordIntegration.INSTANCE.getAddress(packet.getSecret());

        if (address != null) {
            DiscordIntegration.INSTANCE.getPartyManager().shouldAllowUserToJoin(
                target,
                (accepted) -> {
                    if (accepted) {
                        // If the target was accepted, let's add them to the list of invited users...
                        final SPSManager spsManager = connectionManager.getSpsManager();
                        final HashSet<UUID> users = new HashSet<>(spsManager.getInvitedUsers());
                        users.add(target);

                        spsManager.updateInvitedUsers(users);
                        connectionManager.call(new ClientSocialDiscordRequestJoinServerResponsePacket(address))
                            .inResponseTo(packet)
                            .fireAndForget();
                    }

                    return null;
                }
            );
        }
    }
}