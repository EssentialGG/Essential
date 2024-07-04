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
package gg.essential.event.entity;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerTickEvent {
    private final boolean pre;
    private final EntityPlayer player;

    public PlayerTickEvent(boolean pre, EntityPlayer player) {
        this.pre = pre;
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public boolean isPre() {
        return pre;
    }
}
