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
package gg.essential.network.connectionmanager.ice.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.local.LocalChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ice4j.pseudotcp.PseudoTcpSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Links one end of a {@link LocalChannel} pair (the other one is used by MC) up to a {@link PseudoTcpSocket}.
 */
public class PseudoTcpChannelInitializer extends ChannelInitializer<LocalChannel> {
    private static final Logger LOGGER = LogManager.getLogger();

    // Poison element we push onto the outbound queue to signal the end to the writer thread
    private static final ByteBuf CLOSE_MARKER = Unpooled.buffer(0, 0);

    // Ice4j's pseudo TCP implementation does not actually implement socket shutdown. So, to avoid having to wait
    // for timeout each time, we send a specially crafted packet which we can detect on the other side and then close
    // from there as well.
    private static final ByteBuf CLOSE_PACKET = Unpooled.buffer(16);
    static {
        // Just need something unique but constant
        UUID uuid = UUID.nameUUIDFromBytes("Essential Close Packet".getBytes(StandardCharsets.UTF_8));
        CLOSE_PACKET.writeLong(uuid.getLeastSignificantBits());
        CLOSE_PACKET.writeLong(uuid.getMostSignificantBits());
    }

    private final PseudoTcpSocket socket;
    private final UUID user;

    public PseudoTcpChannelInitializer(PseudoTcpSocket socket, UUID user) {
        this.socket = socket;
        this.user = user;
    }

    @Override
    protected void initChannel(LocalChannel channel) {
        LinkedBlockingDeque<ByteBuf> outbound = new LinkedBlockingDeque<>();

        Thread writer = new Thread(() -> {
            try {
                OutputStream outputStream = this.socket.getOutputStream();
                while (true) {
                    ByteBuf buf = outbound.take();
                    if (buf == CLOSE_MARKER) {
                        LOGGER.info("Closing.");
                        // Flush before and after ensures that our close packet actually gets its own UDP packet
                        outputStream.flush();
                        CLOSE_PACKET.getBytes(0, outputStream, CLOSE_PACKET.readableBytes());
                        outputStream.flush();
                        return;
                    }
                    try {
                        buf.readBytes(outputStream, buf.readableBytes());
                    } finally {
                        buf.release();
                    }
                }
            } catch (IOException | InterruptedException e) {
                if (channel.isOpen()) {
                    e.printStackTrace();
                }
            }
        });
        writer.setName("netty->pseudotcp (" + user + ")");
        writer.setDaemon(true);

        Thread reader = new Thread(() -> {
            try {
                InputStream inputStream = this.socket.getInputStream();
                while (channel.isOpen()) {
                    ByteBuf buf = channel.alloc().buffer();
                    try {
                        buf.writeBytes(inputStream, buf.writableBytes());
                        if (ByteBufUtil.equals(buf, CLOSE_PACKET)) {
                            LOGGER.info("Closing.");
                            channel.close();
                            return;
                        }
                        channel.writeAndFlush(buf.retain());
                    } finally {
                        buf.release();
                    }
                }
            } catch (IOException e) {
                if (channel.isOpen()) {
                    e.printStackTrace();
                }
            }
        });
        reader.setName("netty<-pseudotcp (" + user + ")");
        reader.setDaemon(true);

        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);

                reader.start();
                writer.start();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof ByteBuf) {
                    outbound.add((ByteBuf) msg);
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);

                LOGGER.info("{} channel inactive, closing.", user);

                // Stop reader and writer threads
                outbound.add(CLOSE_MARKER);
                reader.interrupt();

                // Give them some time to cleanly shutdown
                writer.join(10_000);
                reader.join(10_000);

                // Finally close (that is, kill) the socket
                LOGGER.info("{} closing PseudoTCP.", user);
                socket.close();

                LOGGER.info("{} closed.", user);
            }
        });
    }
}
