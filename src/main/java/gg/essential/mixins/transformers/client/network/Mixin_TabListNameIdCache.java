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
package gg.essential.mixins.transformers.client.network;

import com.mojang.authlib.GameProfile;
import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(NetHandlerPlayClient.class)
public abstract class Mixin_TabListNameIdCache implements NetHandlerPlayClientExt {
    @Unique
    private final Map<String, UUID> nameToIdCache = new HashMap<>();

    // FIXME preprocessor bug: doesn't search interfaces for mappings when mapping inject target
    //#if FABRIC
    //$$ @Inject(method = "onPlayerList", at = @At("RETURN"))
    //#elseif MC>=11903
    //$$ @Inject(method = "handlePlayerInfoUpdate", at = @At("RETURN"))
    //#else
    @Inject(
        //#if MC<11700
        method = "handlePlayerListItem",
        //#else
        //$$ method = "handlePlayerInfo",
        //#endif
        at = @At("TAIL")
    )
    //#endif
    private void onPlayerListItem(SPacketPlayerListItem packetIn, CallbackInfo ci) {
        //#if MC>=12002
        //$$ for (PlayerListS2CPacket.Entry data : packetIn.getPlayerAdditionEntries()) {
        //#else
        for (SPacketPlayerListItem.AddPlayerData data : packetIn.getEntries()) {
        //#endif
            //#if MC>=11903
            //$$ GameProfile profile = data.profile();
            //#else
            GameProfile profile = data.getProfile();
            //#endif
            // Only real use for this is Hypixel SkyBlock and they send UUID version 4 for normal players, let's not overload the cache
            if (profile.getName() != null && profile.getId().version() == 4) {
                nameToIdCache.put(profile.getName(), profile.getId());
            }
        }
    }

    @NotNull
    @Override
    public Map<String, UUID> essential$getNameIdCache() {
        return nameToIdCache;
    }
}
