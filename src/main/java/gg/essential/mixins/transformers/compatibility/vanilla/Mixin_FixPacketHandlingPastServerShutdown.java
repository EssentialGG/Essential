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
package gg.essential.mixins.transformers.compatibility.vanilla;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IThreadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The MC server executor immediately runs all submitted tasks when the server is already shut down.
 * Which is trivially unsafe for a plethora of reasons, one of which being that packets will queue themselves if they
 * are not being handled on the main thread. But enqueuing will just directly execute them, quickly resulting in a stack
 * overflow.
 * That one's actually pretty likely to happen in SPS and can sometimes show in the disconnect message, so we fix it.
 */
@Mixin(PacketThreadUtil.class)
public abstract class Mixin_FixPacketHandlingPastServerShutdown {
    //#if MC>=11600
    //$$ @Inject(method = "checkThreadAndEnqueue(Lnet/minecraft/network/IPacket;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/concurrent/ThreadTaskExecutor;)V", at = @At("HEAD"))
    //#else
    @Inject(method = "checkThreadAndEnqueue", at = @At("HEAD"))
    //#endif
    private static <T extends INetHandler> void ignoreIfServerIsShutDown(Packet<T> packet, T handler, IThreadListener executor, CallbackInfo ci) {
        MinecraftServer server = null;
        if (executor instanceof MinecraftServer) {
            server = (MinecraftServer) executor;
        //#if MC<11600
        } else if (executor instanceof net.minecraft.world.WorldServer) {
            server = ((net.minecraft.world.WorldServer) executor).getMinecraftServer();
        //#endif
        }
        if (server != null && server.isServerStopped()) {
            throw ThreadQuickExitException.INSTANCE;
        }
    }
}
