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

import io.netty.buffer.Unpooled;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketJoinGame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(NetHandlerPlayClient.class)
public abstract class Mixin_RegisterEssentialChannel {
    @Shadow @Final
    private NetworkManager netManager;

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
        this.netManager.sendPacket(new CPacketCustomPayload(
            //#if MC>=11600
            //$$ new net.minecraft.util.ResourceLocation("register"),
            //#else
            "REGISTER",
            //#endif
            new PacketBuffer(Unpooled.buffer().writeBytes("essential:".getBytes(StandardCharsets.UTF_8)))
        ));
    }
}
