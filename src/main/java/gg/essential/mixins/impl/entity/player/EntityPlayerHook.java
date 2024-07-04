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
package gg.essential.mixins.impl.entity.player;

import gg.essential.Essential;
import gg.essential.event.entity.PlayerTickEvent;
import gg.essential.mixins.impl.ClassHook;
import net.minecraft.entity.player.EntityPlayer;

public class EntityPlayerHook extends ClassHook<EntityPlayer> {

    public EntityPlayerHook(EntityPlayer instance) {
        super(instance);
    }

    public void tickPre() {
        Essential.EVENT_BUS.post(new PlayerTickEvent(true, instance));
    }

    public void tickPost() {
        Essential.EVENT_BUS.post(new PlayerTickEvent(false, instance));
    }
}
