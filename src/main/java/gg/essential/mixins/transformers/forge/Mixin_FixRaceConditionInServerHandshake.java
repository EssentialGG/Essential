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
//#if MC<=11202

import io.netty.channel.EventLoop;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

/**
 * It is in general not safe to call `setConnectionState` from outside the channel's event loop, because it may cause
 * packets which are still being processed to see the wrong state, and consequently fail to serialize.
 *
 * But Forge does so in its server handshake code, and thereby creates a race condition cause the LoginSuccess packet
 * is sent right before that and if it is not encoded by the time forge calls `setConnectionState`, it fails to
 * serialize because "Can't serialize unregistered packet".
 * To reliably reproduce this issue, put a thread-only breakpoint with the condition
 * `packet instanceof SPacketLoginSuccess` at the very start of `NettyPacketEncoder.encode`, try to join via LAN/UPnP,
 * let it hit the breakpoint, wait a second and continue.
 *
 * To fix the issue, we schedule the function call to instead happen on the event loop. This guarantees that any packets
 * already submitted will have been dealt with by the time we get to execute.
 */
@Mixin(value = NetworkDispatcher.class, remap = false)
public abstract class Mixin_FixRaceConditionInServerHandshake {
    @Redirect(method = "serverInitiateHandshake", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;setConnectionState(Lnet/minecraft/network/EnumConnectionState;)V", remap = true))
    private void safelySetConnectionState(NetworkManager networkManager, EnumConnectionState newState) {
        EventLoop eventLoop = networkManager.channel().eventLoop();
        if (eventLoop.inEventLoop()) {
            // Already in event loop, all good
            networkManager.setConnectionState(newState);
            return;
        }

        CompletableFuture<?> future = new CompletableFuture<>();
        eventLoop.execute(() -> {
            networkManager.setConnectionState(newState);
            future.complete(null);
        });

        // I do not know if any code after this one relies on the updated connection state, so we'll wait for it
        // just to be safe.
        future.join();
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_FixRaceConditionInServerHandshake  {
//$$ }
//#endif
