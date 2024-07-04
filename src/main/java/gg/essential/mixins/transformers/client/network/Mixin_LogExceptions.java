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
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class Mixin_LogExceptions {

    @Inject(
        method = "exceptionCaught",
        //#if MC>11600
        //$$ at = @At(value = "INVOKE", target = "Lio/netty/channel/Channel;isOpen()Z"),
        //#else
        at = @At("HEAD"),
        //#endif
        remap = false
    )
    private void essential$logException(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo ci) {
        if (channelHandlerContext.channel().isOpen()) {
            Essential.logger.error("Network Exception Caught", throwable);
        }
    }
}
