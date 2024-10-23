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
package gg.essential.mixins.transformers.client.gui;

import net.minecraft.client.gui.ServerListEntryLanDetected;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ServerSelectionList.class)
public interface ServerSelectionListAccessor {
    @Accessor
    List<ServerListEntryNormal> getServerListInternet();

    @Accessor
    List<ServerListEntryLanDetected> getServerListLan();

    //#if MC<11600
    @Invoker
    int invokeGetSize();
    //#endif

    //#if MC>=11600
    //$$ @Invoker("setList")
    //$$ void updateList();
    //#endif
}
