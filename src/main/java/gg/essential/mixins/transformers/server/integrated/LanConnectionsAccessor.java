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
package gg.essential.mixins.transformers.server.integrated;

import net.minecraft.entity.player.EntityPlayerMP;
//#if MC>11200
import net.minecraft.server.management.PlayerList;
//#else
//$$ import net.minecraft.server.management.ServerConfigurationManager;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//#if MC>11200
@Mixin(PlayerList.class)
//#else
//$$ @Mixin(ServerConfigurationManager.class)
//#endif
public interface LanConnectionsAccessor {

    @Accessor
    List<EntityPlayerMP> getPlayerEntityList();
}
