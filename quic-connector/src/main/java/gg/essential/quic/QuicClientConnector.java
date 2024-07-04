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

import gg.essential.config.AccessedViaReflection;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicChannelBootstrap;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@AccessedViaReflection("ForkedJvmClientQuicStream")
public class QuicClientConnector extends QuicConnector {

    private static final QuicSslContext quicSslContext = QuicSslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE) // MC deals with encryption by itself
        .applicationProtocols("minecraft")
        .build();

    private static final ChannelHandler quicCodec = new QuicClientCodecBuilder()
        .sslContext(quicSslContext)
        // See https://www.rfc-editor.org/rfc/rfc9000.html#name-transport-parameter-definit
        .maxIdleTimeout(30, TimeUnit.SECONDS)
        .initialMaxData(10_000_000)
        .initialMaxStreamDataBidirectionalLocal(10_000_000)
        .build();

    private final LogOnce debugOnce = LogOnce.toForkedJvmDebug();

    private QuicClientConnector() {
    }

    /**
     * Binds a new QUIC proxy which accepts application layer connections and sends its transport layer packets to the
     * given port.
     * Returns the port on which the QUIC proxy expects transport layer packets, as well as the port on which it expects
     * the application layer connection, as well as the port on which it expects http connections.
     *
     * No packets are sent on the transport layer until the application layer connects to the returned port.
     */
    public int[] bindProxy(String host, int icePort) {
        EventLoop eventLoop = group.next(); // same thread for both vastly simplifies thread safety

        // Bind the channel which acts as our transport layer
        Channel udpChannel = new Bootstrap()
            .group(eventLoop)
            .channel(NioDatagramChannel.class)
            .handler(new LogOnceHandler(LogOnce.toForkedJvmDebug(), "transport"))
            .bind(host, 0)
            .syncUninterruptibly()
            .channel();

        udpChannel.pipeline().addLast(quicCodec);

        // Prepare the QUIC channel which we'll be initializing once the application has connected
        QuicChannelBootstrap quicChannelBootstrap = QuicChannel.newBootstrap(udpChannel)
            .streamHandler(new ChannelInboundHandlerAdapter())
            .remoteAddress(new InetSocketAddress(host, icePort));

        // Listen for http clients to connect
        HttpProxyInitializer httpProxy = new HttpProxyInitializer();
        Channel httpChannel = new ServerBootstrap()
            .group(eventLoop)
            .channel(NioServerSocketChannel.class)
            .childHandler(httpProxy)
            .childOption(ChannelOption.AUTO_READ, false) // delay reading until we are connected on both ends
            .bind(host, 0)
            .syncUninterruptibly()
            .channel();

        // Listen for the application to connect
        Channel tcpChannel = new ServerBootstrap()
            .group(eventLoop)
            .channel(NioServerSocketChannel.class)
            .childHandler(new McProxyInitializer(host, udpChannel, quicChannelBootstrap, httpChannel, httpProxy))
            .childOption(ChannelOption.AUTO_READ, false) // delay reading until we are connected on both ends
            .bind(host, 0)
            .syncUninterruptibly()
            .channel();

        int udpPort = ((InetSocketAddress) udpChannel.localAddress()).getPort();
        int tcpPort = ((InetSocketAddress) tcpChannel.localAddress()).getPort();
        int httpPort = ((InetSocketAddress) httpChannel.localAddress()).getPort();
        return new int[] { udpPort, tcpPort, httpPort };
    }

    private class McProxyInitializer extends ChannelInitializer<SocketChannel> {

        private final String host;
        private final Channel udpChannel;
        private final QuicChannelBootstrap quicChannelBootstrap;
        private final Channel httpChannel;
        private final HttpProxyInitializer httpProxy;

        public McProxyInitializer(String host, Channel udpChannel, QuicChannelBootstrap quicChannelBootstrap, Channel httpChannel, HttpProxyInitializer httpProxy) {
            this.host = host;
            this.udpChannel = udpChannel;
            this.quicChannelBootstrap = quicChannelBootstrap;
            this.httpChannel = httpChannel;
            this.httpProxy = httpProxy;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            debugOnce.log("initChannel", ch.remoteAddress());
            ch.pipeline().addLast(new McProxyFrontendHandler(host, udpChannel, quicChannelBootstrap, httpChannel, httpProxy));
        }
    }

    private class McProxyFrontendHandler extends ProxyHandler {

        private final String host;
        private final Channel udpChannel;
        private final QuicChannelBootstrap quicChannelBootstrap;
        private final Channel httpChannel;
        private final HttpProxyInitializer httpProxy;

        public McProxyFrontendHandler(String host, Channel udpChannel, QuicChannelBootstrap quicChannelBootstrap, Channel httpChannel, HttpProxyInitializer httpProxy) {
            this.host = host;
            this.udpChannel = udpChannel;
            this.quicChannelBootstrap = quicChannelBootstrap;
            this.httpChannel = httpChannel;
            this.httpProxy = httpProxy;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // Application has connected,
            Channel tcpChannel = ctx.channel();
            // start connecting our QUIC client to the remote side
            debugOnce.log("connect");
            quicChannelBootstrap
                .connect()
                .addListener((GenericFutureListener<Future<QuicChannel>>) quicChannelFuture -> {
                    if (!quicChannelFuture.isSuccess()) {
                        quicChannelFuture.cause().printStackTrace();
                        tcpChannel.close();
                        return;
                    }
                    // QUIC channel is connected
                    QuicChannel quicChannel = quicChannelFuture.getNow();

                    // we need just one bidirectional stream
                    debugOnce.log("createStream");
                    quicChannel
                        .createStream(QuicStreamType.BIDIRECTIONAL, new McProxyBackendHandler(tcpChannel))
                        .addListener((GenericFutureListener<Future<QuicStreamChannel>>) streamChannelFuture -> {
                            if (!streamChannelFuture.isSuccess()) {
                                streamChannelFuture.cause().printStackTrace();
                                tcpChannel.close();
                                quicChannel.close();
                                return;
                            }

                            targetChannel = streamChannelFuture.getNow();

                            // connection complete, begin reading data from frontend channel
                            if (tcpChannel.isActive()) {
                                debugOnce.log("setAutoRead");
                                tcpChannel.config().setAutoRead(true);
                            }
                        });

                    // Also start serving local http connections
                    httpProxy.quicChannel = quicChannel;
                });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            debugOnce.log("channelInactive");
            if (targetChannel == null) {
                // MC client disconnected while we were connecting, clean up and quit
                udpChannel.close();
                httpChannel.close();
            }
            super.channelInactive(ctx);
        }
    }

    private static class McProxyBackendHandler extends ProxyHandler {
        public McProxyBackendHandler(Channel tcpChannel) {
            super(tcpChannel);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            // Once our main QUIC stream is closed, close the whole QUIC channel
            if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
                ((QuicChannel) ctx.channel().parent()).close(true, 0, Unpooled.EMPTY_BUFFER);
            }
        }
    }

    private static class HttpProxyInitializer extends ChannelInitializer<SocketChannel> {

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

    private static class HttpProxyFrontendHandler extends ProxyHandler {

        private final QuicChannel quicChannel;

        public HttpProxyFrontendHandler(QuicChannel quicChannel) {
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
                        streamChannelFuture.cause().printStackTrace();
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

    public static void main(String[] args) throws IOException {
        QuicClientConnector connector = new QuicClientConnector();
        try {
            DataOutputStream out = new DataOutputStream(stdOut);
            DataInputStream in = new DataInputStream(System.in);

            String host = in.readUTF();
            int icePort = in.readUnsignedShort();
            int[] ports = connector.bindProxy(host, icePort);
            out.writeShort(ports[0]); // udp port
            out.writeShort(ports[1]); // tcp port
            out.writeShort(ports[2]); // http port
            out.flush();

            //noinspection ResultOfMethodCallIgnored
            in.read(); // wait until parent signals us to quit or terminates itself
        } finally {
            connector.group.shutdownGracefully();
        }
    }
}
