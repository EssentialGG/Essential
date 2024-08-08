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
package gg.essential.network.connectionmanager.ice.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.local.LocalChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.*

class CoroutinesChannelInitializer(
    private val coroutineScope: CoroutineScope,
    private val inboundChannel: ReceiveChannel<ByteArray>,
    private val outboundChannel: SendChannel<ByteArray>,
    private val onClose: () -> Unit,
) : ChannelInitializer<LocalChannel>() {
    override fun initChannel(channel: LocalChannel) {
        val reader = coroutineScope.launch(start = CoroutineStart.LAZY) {
            try {
                for (bytes in inboundChannel) {
                    channel.writeAndFlush(Unpooled.wrappedBuffer(bytes))
                }
            } finally {
                channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }

        // The transport channels implement backpressure, however MC doesn't really, so we need an infinite
        // buffer inbetween so we don't end up blocking the netty thread.
        val bufferedOutboundChannel = Channel<ByteArray>(Channel.UNLIMITED)
        coroutineScope.launch(Dispatchers.Unconfined) {
            try {
                for (bytes in bufferedOutboundChannel) {
                    outboundChannel.send(bytes)
                }
            } catch (e: Exception) {
                outboundChannel.close(e)
                throw e
            } finally {
                outboundChannel.close()
            }
        }

        channel.pipeline().addLast(object : ChannelInboundHandlerAdapter() {
            @Throws(Exception::class)
            override fun channelActive(ctx: ChannelHandlerContext) {
                super.channelActive(ctx)

                reader.start()
            }

            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                if (msg is ByteBuf) {
                    val byteArray = ByteArray(msg.readableBytes())
                    msg.readBytes(byteArray)
                    msg.release()
                    bufferedOutboundChannel.trySend(byteArray)
                    return
                }
                super.channelRead(ctx, msg)
            }

            @Throws(Exception::class)
            override fun channelInactive(ctx: ChannelHandlerContext) {
                super.channelInactive(ctx)

                bufferedOutboundChannel.close()
                onClose()
            }
        })
    }
}