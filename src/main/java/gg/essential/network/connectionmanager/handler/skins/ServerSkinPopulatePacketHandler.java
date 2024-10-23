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
package gg.essential.network.connectionmanager.handler.skins;

import gg.essential.connectionmanager.common.packet.skin.ServerSkinPopulatePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import gg.essential.network.connectionmanager.skins.SkinItem;
import gg.essential.network.cosmetics.ConversionsKt;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerSkinPopulatePacketHandler extends PacketHandler<ServerSkinPopulatePacket> {

    @Override
    protected void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final ServerSkinPopulatePacket packet) {
        connectionManager.getSkinsManager().onSkinPopulate(packet.getSkins().stream().map(ConversionsKt::toMod).collect(Collectors.toMap(SkinItem::getId, Function.identity())));
    }

}
