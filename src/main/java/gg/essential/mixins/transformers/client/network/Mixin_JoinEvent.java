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

import gg.essential.Essential;
import gg.essential.event.network.server.ServerJoinEvent;
import gg.essential.event.network.server.SingleplayerJoinEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketJoinGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class Mixin_JoinEvent {
    // FIXME preprocessor bug: doesn't search interfaces for mappings when mapping inject target
    //#if FABRIC
    //$$ @Inject(method = "onGameJoin", at = @At("RETURN"))
    //#else
    @Inject(
        //#if MC<11700
        method = "handleJoinGame",
        //#else
        //$$ method = "handleLogin",
        //#endif
        at = @At("RETURN")
    )
    //#endif
    private void onJoinGame(SPacketJoinGame packetIn, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        ServerData serverData = mc.getCurrentServerData();
        if (serverData != null) {
            Essential.EVENT_BUS.post(new ServerJoinEvent(serverData));
        } else if (mc.isIntegratedServerRunning()) {
            Essential.EVENT_BUS.post(new SingleplayerJoinEvent());
        }
    }
}
