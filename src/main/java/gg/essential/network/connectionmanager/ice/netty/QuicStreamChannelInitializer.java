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

import gg.essential.quic.LogOnce;
import gg.essential.sps.quic.QuicStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.local.LocalChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Links one end of a {@link LocalChannel} pair (the other one is used by MC) up to a {@link QuicStream}.
 */
public class QuicStreamChannelInitializer extends ChannelInitializer<LocalChannel> {
    private static final Logger LOGGER = LogManager.getLogger();

    // Poison element we push onto the outbound queue to signal the end to the writer thread
    private static final ByteBuf CLOSE_MARKER = Unpooled.buffer(0, 0);
    private final LogOnce debugOnce;

    private final QuicStream quicStream;
    private final UUID user;

    public QuicStreamChannelInitializer(QuicStream quicStream, UUID user) {
        this.quicStream = quicStream;
        this.user = user;
        this.debugOnce = LogOnce.to(s -> LOGGER.debug("[{}] {}", user, s));
    }

    @Override
    protected void initChannel(LocalChannel channel) {
        debugOnce.log("initChannel", channel);

        LinkedBlockingDeque<ByteBuf> outbound = new LinkedBlockingDeque<>();

        Thread writer = new Thread(() -> {
            try {
                OutputStream outputStream = this.quicStream.getOutputStream();
                while (true) {
                    ByteBuf buf = outbound.take();
                    debugOnce.log("writer", buf);
                    if (buf == CLOSE_MARKER) {
                        LOGGER.info("Closing.");
                        outputStream.close();
                        return;
                    }
                    try {
                        buf.readBytes(outputStream, buf.readableBytes());
                    } finally {
                        buf.release();
                    }
                }
            } catch (IOException | InterruptedException e) {
                debugOnce.log("writerException", e);
                if (channel.isOpen()) {
                    e.printStackTrace();
                }
            }
        });
        writer.setName("netty->quic (" + user + ")");
        writer.setDaemon(true);

        Thread reader = new Thread(() -> {
            try {
                InputStream inputStream = this.quicStream.getInputStream();
                while (channel.isOpen()) {
                    ByteBuf buf = channel.alloc().buffer();
                    try {
                        if (buf.writeBytes(inputStream, buf.writableBytes()) <= 0) {
                            LOGGER.info("Closing.");
                            channel.close();
                            return;
                        }
                        debugOnce.log("reader", buf);
                        channel.writeAndFlush(buf.retain());
                    } finally {
                        buf.release();
                    }
                }
            } catch (IOException e) {
                debugOnce.log("readerException", e);
                if (channel.isOpen()) {
                    e.printStackTrace();
                }
            }
        });
        reader.setName("netty<-quic (" + user + ")");
        reader.setDaemon(true);

        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                debugOnce.log("channelActive", ctx);

                super.channelActive(ctx);

                reader.start();
                writer.start();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                debugOnce.log("channelRead", msg);
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

                // Finally, close the quic stream
                LOGGER.info("{} closing QUIC.", user);
                quicStream.close();

                LOGGER.info("{} closed.", user);
            }
        });
    }
}
