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
package gg.essential.quic.backend;

import gg.essential.config.AccessedViaReflection;
import gg.essential.quic.LogOnce;
import gg.essential.quic.ProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static gg.essential.quic.QuicUtil.LOCALHOST;

@AccessedViaReflection("QuicBackendLoader")
public class QuicBackendImpl implements QuicBackend {
    static {
        InternalLoggerFactory.setDefaultFactory(new WrappedLoggingFactory(InternalLoggerFactory.getDefaultFactory()));
    }

    private static final QuicSslContext quicClientSslContext = QuicSslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE) // MC deals with encryption by itself
        .applicationProtocols("minecraft")
        .build();

    private static final QuicClientCodecBuilder quicClientCodec = new QuicClientCodecBuilder()
        .sslContext(quicClientSslContext)
        // See https://www.rfc-editor.org/rfc/rfc9000.html#name-transport-parameter-definit
        .maxIdleTimeout(30, TimeUnit.SECONDS)
        .initialMaxData(10_000_000)
        .initialMaxStreamDataBidirectionalLocal(10_000_000)
        ;

    private static final SelfSignedCert certificate;
    static {
        try {
            certificate = new SelfSignedCert();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final QuicSslContext quicServerSslContext = QuicSslContextBuilder
        .forServer(certificate.privateKey(), null, certificate.certificate())
        .applicationProtocols("minecraft")
        .build();

    private static final QuicServerCodecBuilder quicServerCodecBuilder = new QuicServerCodecBuilder()
        .sslContext(quicServerSslContext)
        .tokenHandler(InsecureQuicTokenHandler.INSTANCE) // ICE validates the target addresses
        // See https://www.rfc-editor.org/rfc/rfc9000.html#name-transport-parameter-definit
        .maxIdleTimeout(30, TimeUnit.SECONDS)
        .initialMaxData(10_000_000)
        .initialMaxStreamDataBidirectionalRemote(10_000_000)
        .initialMaxStreamsBidirectional(10)
        ;

    // Arbitrary addresses we can pass to netty's QUIC codec
    private static final InetSocketAddress QUIC_LOCAL_ADDRESS = new InetSocketAddress("172.16.10.10", 10000);
    private static final InetSocketAddress QUIC_REMOTE_ADDRESS = new InetSocketAddress("172.16.10.10", 10001);

    private final NioEventLoopGroup group = new NioEventLoopGroup(1,
        new DefaultThreadFactory("QUIC Backend", true));

    private final Logger logger;
    private final QuicListener listener;

    private final EventLoop eventLoop = group.next();

    private Channel transportChannel;
    private QuicChannel quicChannel;
    private QuicStreamChannel streamChannel;

    public QuicBackendImpl(Logger logger, QuicListener listener) {
        this.logger = logger;
        this.listener = listener;
    }

    public int connect() {
        Channel udpTransportChannel = new ServerBootstrap()
            .channel(LocalServerChannel.class)
            .group(eventLoop)
            .childHandler(new ChannelInitializer<LocalChannel>() {
                @Override
                protected void initChannel(LocalChannel ch) throws Exception {
                    ch.pipeline().addLast(new UdpProxyHandler(logger, listener));
                    transportChannel = ch;
                }
            })
            .bind(LocalAddress.ANY)
            .awaitUninterruptibly()
            .channel();

        Channel quicConnectionChannel = new Bootstrap()
            .channel(LocalChannel.class)
            .group(eventLoop)
            .handler(quicClientCodec.build())
            .connect(udpTransportChannel.localAddress())
            .awaitUninterruptibly()
            .channel();

        // Listen for http clients to connect
        HttpProxyInitializer httpProxy = new HttpProxyInitializer();
        Channel httpChannel = new ServerBootstrap()
            .group(eventLoop)
            .channel(NioServerSocketChannel.class)
            .childHandler(httpProxy)
            .childOption(ChannelOption.AUTO_READ, false) // delay reading until we are connected on both ends
            .bind(LOCALHOST, 0)
            .syncUninterruptibly()
            .channel();

        QuicChannel.newBootstrap(quicConnectionChannel)
            .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    // Close our side of the transport
                    ctx.channel().parent().close();
                }
            })
            .localAddress(QUIC_LOCAL_ADDRESS)
            .remoteAddress(QUIC_REMOTE_ADDRESS)
            .connect()
            .addListener((GenericFutureListener<Future<QuicChannel>>) quicChannelFuture -> {
                if (!quicChannelFuture.isSuccess()) {
                    logger.error("Failed to create QUIC channel", quicChannelFuture.cause());
                    finishClose();
                    return;
                }
                // QUIC channel is connected
                QuicChannel quicChannel = quicChannelFuture.getNow();

                // we just need one bidirectional stream
                quicChannel.createStream(QuicStreamType.BIDIRECTIONAL, new McProxyHandler(listener))
                    .addListener((GenericFutureListener<Future<QuicStreamChannel>>) streamChannelFuture -> {
                    if (!streamChannelFuture.isSuccess()) {
                        logger.error("Failed to create QUIC stream", streamChannelFuture.cause());
                        quicChannel.close();
                        finishClose();
                        return;
                    }

                    streamChannel = streamChannelFuture.getNow();

                    listener.onOpen();
                });

                this.quicChannel = quicChannel;
                httpProxy.quicChannel = quicChannel;
            });

        return ((InetSocketAddress) httpChannel.localAddress()).getPort();
    }

    public void accept(int httpPort) {
        Channel udpTransportChannel = new ServerBootstrap()
            .channel(LocalServerChannel.class)
            .group(eventLoop)
            .childHandler(new ChannelInitializer<LocalChannel>() {
                @Override
                protected void initChannel(LocalChannel ch) throws Exception {
                    ch.pipeline().addLast(new UdpProxyHandler(logger, listener));
                    transportChannel = ch;
                }
            })
            .bind(LocalAddress.ANY)
            .awaitUninterruptibly()
            .channel();

        Channel quicConnectionChannel = new Bootstrap()
            .channel(LocalChannel.class)
            .group(eventLoop)
            .handler(new ChannelInboundHandlerAdapter())
            .connect(udpTransportChannel.localAddress())
            .awaitUninterruptibly()
            .channel();

        ChannelHandler serverCodec = quicServerCodecBuilder
            .clone()
            .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    // Close our side of the transport
                    ctx.channel().parent().close();
                }
            })
            .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                boolean waitingForInitialChannel = true;

                @Override
                protected void initChannel(QuicStreamChannel ch) throws Exception {
                    if (waitingForInitialChannel) {
                        waitingForInitialChannel = false;
                        streamChannel = ch;

                        ch.pipeline().addLast(new McProxyHandler(listener) {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                listener.onOpen();
                            }
                        });
                    } else {
                        // Delay reading until the tcp proxy is ready
                        ch.config().setAutoRead(false);
                        ch.pipeline().addLast(new TcpProxyFrontendHandler(LOCALHOST, httpPort));
                    }
                }
            })
            .build();

        quicConnectionChannel.pipeline().addLast(serverCodec);
    }

    public void transportRecv(byte[] packet) {
        ByteBuf buf = Unpooled.wrappedBuffer(packet);
        transportChannel.writeAndFlush(new DatagramPacket(buf, QUIC_LOCAL_ADDRESS, QUIC_REMOTE_ADDRESS));
    }

    public void quicSend(byte[] buf) {
        streamChannel.writeAndFlush(Unpooled.wrappedBuffer(buf));
    }

    @Override
    public void close() throws IOException {
        if (streamChannel != null) {
            streamChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        } else {
            if (quicChannel != null) {
                quicChannel.close();
            }
            finishClose();
        }
    }

    private void finishClose() {
        // Delay close actions by 1 second to allow for retransmits of the final CONNECTION_CLOSE frame
        eventLoop.schedule(() -> {
            if (transportChannel != null) {
                transportChannel.close();
            }
            group.shutdownGracefully();
            listener.onClosed();
        }, 1, TimeUnit.SECONDS);
    }

    private class UdpProxyHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final Logger logger;
        private final QuicListener listener;

        private UdpProxyHandler(Logger logger, QuicListener listener) {
            this.logger = logger;
            this.listener = listener;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
            ByteBuf msg = packet.content();
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            listener.transportSend(bytes);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            finishClose();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("QUIC UDP Forwarding Error", cause);
            if (ctx.channel().isActive()) {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private class McProxyHandler extends ChannelDuplexHandler {
        private final QuicListener listener;

        private McProxyHandler(QuicListener listener) {
            this.listener = listener;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            byteBuf.release();
            listener.quicRecv(bytes);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            // Once our main QUIC stream is closed, close the whole QUIC channel
            if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
                logger.debug("QUIC stream closed. Closing QUIC channel.");
                listener.onReceivingStreamClosed();
                ctx.close().addListener(future -> {
                    ((QuicChannel) ctx.channel().parent()).close(true, 0, Unpooled.EMPTY_BUFFER);
                });
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("QUIC MC Forwarding Error", cause);
            if (ctx.channel().isActive()) {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private class HttpProxyInitializer extends ChannelInitializer<SocketChannel> {

        public QuicChannel quicChannel;

        @Override
        protected void initChannel(SocketChannel ch) {
            if (quicChannel == null) {
                ch.close();
                return;
            }
            ch.pipeline().addLast(new HttpProxyFrontendHandler(quicChannel));
        }
    }

    private class HttpProxyFrontendHandler extends ProxyHandler {
        private final QuicChannel quicChannel;

        public HttpProxyFrontendHandler(QuicChannel quicChannel) {
            super(LogOnce.to(logger::debug), null);
            this.quicChannel = quicChannel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);

            // Application has connected,
            Channel tcpChannel = ctx.channel();

            // hook it up to a quic stream
            quicChannel
                .createStream(QuicStreamType.BIDIRECTIONAL, new ProxyHandler(tcpChannel))
                .addListener((GenericFutureListener<Future<QuicStreamChannel>>) streamChannelFuture -> {
                    if (!streamChannelFuture.isSuccess()) {
                        logger.info("Failed to create QUIC stream", streamChannelFuture.cause());
                        tcpChannel.close();
                        return;
                    }

                    targetChannel = streamChannelFuture.getNow();

                    // connection complete, begin reading data from frontend channel
                    if (tcpChannel.isActive()) {
                        tcpChannel.config().setAutoRead(true);
                    }
                });
        }
    }

    private class TcpProxyFrontendHandler extends ProxyHandler {
        private final int tcpPort;
        private final InetAddress tcpHost;

        public TcpProxyFrontendHandler(InetAddress tcpHost, int tcpPort) {
            super(LogOnce.to(logger::debug), null);
            this.tcpHost = tcpHost;
            this.tcpPort = tcpPort;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // Remote side has connected via QUIC
            Channel quicStreamChannel = ctx.channel();
            // start connecting the application layer
            new Bootstrap()
                .group(quicStreamChannel.eventLoop()) // same thread for both vastly simplifies thread safety
                .channel(NioSocketChannel.class)
                .handler(new ProxyHandler(quicStreamChannel))
                .connect(tcpHost, tcpPort)
                .addListener((ChannelFutureListener) tcpChannelFuture -> {
                    if (!tcpChannelFuture.isSuccess()) {
                        logger.error("Failed to create TCP connection", tcpChannelFuture.cause());
                        quicStreamChannel.close();
                        return;
                    }

                    targetChannel = tcpChannelFuture.channel();

                    if (quicStreamChannel.isActive()) {
                        // connection complete, begin reading data from frontend channel
                        quicStreamChannel.config().setAutoRead(true);
                    } else {
                        // MC client disconnected while we were connecting, clean up and quit
                        targetChannel.close();
                    }
                });
        }
    }
}
