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
package gg.essential.network.pingproxy

import com.google.common.util.concurrent.ThreadFactoryBuilder
import gg.essential.Essential
import gg.essential.connectionmanager.common.packet.pingproxy.ClientPingProxyPacket
import gg.essential.connectionmanager.common.packet.pingproxy.ServerPingProxyResponsePacket
import gg.essential.mixins.ext.client.multiplayer.ext
import gg.essential.mixins.ext.client.multiplayer.pingOverride
import gg.essential.mixins.ext.client.multiplayer.pingRegion
import gg.essential.network.connectionmanager.AsyncResponseHandler
import gg.essential.network.connectionmanager.ice.netty.CloseAfterFirstMessage
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.local.LocalAddress
import io.netty.channel.local.LocalServerChannel
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.network.NettyVarint21FrameDecoder
import net.minecraft.network.PacketBuffer
import java.net.SocketAddress
import java.util.concurrent.TimeUnit

class ProxyPingServer(val serverData: ServerData) {

    val channel: Channel = ServerBootstrap()
        .channel(LocalServerChannel::class.java)
        .group(serverGroup)
        .handler(CloseAfterFirstMessage())
        .childHandler(object : ChannelInitializer<Channel>() {
            override fun initChannel(ch: Channel) {
                ch.pipeline()
                    .addLast("splitter", NettyVarint21FrameDecoder(
                        //#if MC>=12002
                        //$$ null,
                        //#endif
                    ))
                    .addLast("packet_handler", ProxyPingPacketHandler(serverData))
            }
        })
        .localAddress(LocalAddress.ANY)
        .bind()
        .syncUninterruptibly()
        .channel()

    val address: SocketAddress = channel.localAddress()
}

val targetServerData = ThreadLocal<ServerData>()
val serverGroup = makeEventGroup(false)
val clientGroup = makeEventGroup(true)

fun makeEventGroup(client: Boolean): EventLoopGroup {
    //#if MC>=11200
    return io.netty.channel.DefaultEventLoopGroup(
    //#else
    //$$ return io.netty.channel.local.LocalEventLoopGroup(
    //#endif
        0,
        ThreadFactoryBuilder().setNameFormat("Essential Ping Proxy ${if (client) "Client" else "Server"} #%d").setDaemon(true).build()
    )
}

class ProxyPingPacketHandler(val serverData: ServerData): SimpleChannelInboundHandler<ByteBuf>() {

    private var isQuery = false
    private var pingData: ClientPingProxyPacket? = null

    lateinit var channel: Channel

    override fun channelActive(ctx: ChannelHandlerContext) {
        channel = ctx.channel()
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf) {
        val buf = PacketBuffer(msg)
        val id = buf.readVarInt()
        if (!isQuery) {
            if (id == 0x00) { // Handshake
                val protocolVersion = buf.readVarInt()
                val address = buf.readString(255).split("\u0000")[0] // Remove FML marker, which comes after a null byte
                val port = buf.readUnsignedShort()
                val nextState = buf.readVarInt()

                pingData = ClientPingProxyPacket(address, port, protocolVersion)

                if (nextState == 1) {
                    isQuery = true
                } else {
                    Essential.logger.warn("Invalid nextState $nextState sent to pingproxy")
                    channel.close()
                }
            }
        } else {
            if (id == 0x00) { // Status Request
                val pingData = pingData!!
                Essential.getInstance().connectionManager.send(pingData, AsyncResponseHandler { maybeResponse ->
                    val response = maybeResponse.orElse(null) as? ServerPingProxyResponsePacket
                    if (response != null) {
                        sendPacket(0x00) { // Query Response
                            writeString(response.rawJson)
                        }
                        // If we set the ping directly, it will get override with the value calculated in ServerPinger
                        // This override value is copied to the ping field after the calculated value is set, see Mixin_OverridePing
                        serverData.ext.pingOverride = response.latency
                        serverData.ext.pingRegion = response.region
                    } else {
                        // If we didn't get a response from CM, just force disconnect which shows "(no connection)" in the gui
                        channel.close()
                        Essential.logger.info("Received no response from ping proxy for ${pingData.hostname}:${pingData.port} (${pingData.protocolVersion})")
                    }
                }, TimeUnit.SECONDS, 7)
            } else if (id == 0x01) { // Ping
                val payload = buf.readLong()
                sendPacket(0x01) { // Pong
                    writeLong(payload)
                }
            }
        }
    }

    private fun sendPacket(id: Int, writer: PacketBuffer.() -> Unit) {
        val buf = PacketBuffer(Unpooled.buffer())
        buf.writeVarInt(id) // Packet ID
        writer(buf)

        val packet = PacketBuffer(Unpooled.buffer())
        packet.writeVarInt(buf.readableBytes()) // Size
        packet.writeBytes(buf)

        channel.writeAndFlush(packet)
    }
}
