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

import gg.essential.network.client.MinecraftHook;
import gg.essential.universal.UMinecraft;
import gg.essential.util.USession;
import gg.essential.util.UUIDUtil;

import java.util.UUID;

/**
 * Impl of Minecraft Hook for Essential network
 */
public class NetworkHook implements MinecraftHook {

    @Override
    public String getSession() {
        return UMinecraft.getMinecraft().getSession().getToken();
    }

    @Override
    public UUID getPlayerUUID() {
        return UUIDUtil.getClientUUID();
    }

    @Override
    public String getPlayerName() {
        return USession.Companion.activeNow().getUsername();
    }

}
