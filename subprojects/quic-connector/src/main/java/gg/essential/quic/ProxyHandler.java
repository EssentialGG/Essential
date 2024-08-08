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
package gg.essential.quic;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.quic.QuicStreamChannel;

/**
 * Forwards all messages from this channel to a given target channel.
 *
 * The target must be set before the first message is sent.
 * One way to ensure that is to disable auto-read on this channel until the target is available.
 *
 * When this channel is closed (and the target has been set), the target is flushed and shut down as well.
 */
public class ProxyHandler extends LogOnceHandler {
    protected Channel targetChannel;

    public ProxyHandler() {
        this(null);
    }

    public ProxyHandler(Channel targetChannel) {
        this(LogOnce.toForkedJvmDebug(), targetChannel);
    }

    public ProxyHandler(LogOnce logOnce, Channel targetChannel) {
        super(logOnce, null);
        this.targetChannel = targetChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        targetChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
                targetChannel.close();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (targetChannel != null && targetChannel.isActive()) {
            if (targetChannel instanceof QuicStreamChannel) {
                targetChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            } else {
                targetChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
