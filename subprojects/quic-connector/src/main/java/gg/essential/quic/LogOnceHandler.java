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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class LogOnceHandler extends ChannelDuplexHandler {
    private final LogOnce logOnce;
    private final String name;
    private final String readKey;
    private final String writeKey;

    public LogOnceHandler(LogOnce logOnce, String name) {
        this.logOnce = logOnce;
        this.name = name != null ? name : getClass().getSimpleName();
        this.readKey = this.name + ".channelRead";
        this.writeKey = this.name + ".write";
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logOnce.log(readKey, () -> ctx.channel() + " " + msg);
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logOnce.log(writeKey, () -> ctx.channel() + " " + msg);
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logOnce.log(name + ".channelInactive", ctx.channel());
        super.channelInactive(ctx);
    }
}
