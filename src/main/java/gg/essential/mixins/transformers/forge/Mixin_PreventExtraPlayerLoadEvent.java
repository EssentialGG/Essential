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
package gg.essential.mixins.transformers.forge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.mixins.impl.forge.PlayerListHook;
import gg.essential.mixins.transformers.server.Mixin_InvertPlayerDataPriority;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.SaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

/**
 * NOTE: This mixin only affects Forge. It is disabled from the {@link gg.essential.mixins.Plugin} on Fabric.
 * <p>
 * When an attempt to load the player's data from the playerdata folder fails in {@link Mixin_InvertPlayerDataPriority},
 * we fall back to the vanilla way of loading a player's data. This will end up attempting to load from player.dat if
 * the player is the world host, otherwise, it will call `readPlayerData` again, which fires the player loading event,
 * regardless of if the data has been retrieved or not.
 * <p>
 * Considering the inappropriate placement of this event trigger, we should ignore the event if the NBT data is null
 * and the {@link Mixin_InvertPlayerDataPriority} mixin has stated that it will call {@link SaveHandler#readPlayerData(EntityPlayer)}.
 */
@Mixin(SaveHandler.class)
public class Mixin_PreventExtraPlayerLoadEvent {
    @SuppressWarnings({"unused"})
    @WrapWithCondition(
        method = "readPlayerData(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/nbt/NBTTagCompound;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/event/ForgeEventFactory;firePlayerLoadingEvent(Lnet/minecraft/entity/player/EntityPlayer;Ljava/io/File;Ljava/lang/String;)V"
        )
    )
    private boolean essential$preventExtraPlayerLoadEvent(EntityPlayer player, File path, String uuid, @Local NBTTagCompound nbt) {
        // We want to suppress the event when our load fails, but leave it unaltered for the vanilla code paths.
        if (PlayerListHook.suppressForgeEventIfLoadFails && nbt == null) {
            return false;
        } else {
            return true;
        }
    }
}
