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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

@AccessedViaReflection("ForkedJvmServerQuicStreamPool")
public class QuicServerConnector extends QuicConnector {

    // Self-signed certificate will do, we rely on MC to do its regular encryption anyway
    private final SelfSignedCertificate certificate = new SelfSignedCertificate();
    private final QuicSslContext quicSslContext =
        QuicSslContextBuilder.forServer(certificate.privateKey(), null, certificate.certificate())
            .applicationProtocols("minecraft")
            .build();

    private final QuicServerCodecBuilder quicCodecBuilder = new QuicServerCodecBuilder()
        .sslContext(quicSslContext)
        .tokenHandler(InsecureQuicTokenHandler.INSTANCE) // ICE validates the target addresses
        // See https://www.rfc-editor.org/rfc/rfc9000.html#name-transport-parameter-definit
        .maxIdleTimeout(30, TimeUnit.SECONDS)
        .initialMaxData(10_000_000)
        .initialMaxStreamDataBidirectionalRemote(10_000_000)
        .initialMaxStreamsBidirectional(10)
        ;

    private QuicServerConnector() throws CertificateException {
    }

    /**
     * Binds a new QUIC proxy which connects the application layer to the given tcp port.
     * Returns the port on which the QUIC proxy expects transport layer packets.
     *
     * The application layer connection is only opened once the QUIC channel+stream have been connected.
     */
    public int bindProxy(String host, int tcpPort, int httpPort) {
        // Bind the channel which acts as our transport layer
        Channel channel = new Bootstrap()
            .group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ConnectTimeoutHandler())
            .bind(host, 0)
            .syncUninterruptibly()
            .channel();

        channel.pipeline().addLast(new LogOnceHandler(LogOnce.toForkedJvmDebug(), "transport"));

        // Setup a QUIC server on our transport layer channel
        ChannelHandler codec = quicCodecBuilder
            .handler(new ChannelInboundHandlerAdapter())
            .streamHandler(new ProxyInitializer(host, tcpPort, httpPort))
            .build();
        channel.pipeline().addLast(codec);

        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    private static class ConnectTimeoutHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // If they do not connect within a reasonable time frame, then close the channel again
            ctx.channel().pipeline().addFirst(new ReadTimeoutHandler(10, TimeUnit.SECONDS));

            super.channelActive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (cause instanceof ReadTimeoutException) {
                ctx.channel().close();
            } else {
                super.exceptionCaught(ctx, cause);
            }
        }
    }

    private static class ProxyInitializer extends ChannelInitializer<QuicStreamChannel> {

        private final LogOnce debugOnce = LogOnce.toForkedJvmDebug();
        private final String tcpHost;
        private final int tcpPort;
        private final int httpPort;

        private boolean waitingForInitialStream = true;

        public ProxyInitializer(String tcpHost, int tcpPort, int httpPort) {
            this.tcpHost = tcpHost;
            this.tcpPort = tcpPort;
            this.httpPort = httpPort;
        }

        @Override
        protected void initChannel(QuicStreamChannel ch) {
            debugOnce.log("initQuicStreamChannel", ch);

            ch.config().setAutoRead(false); // delay reading until we are connected on both ends

            // First stream is MC, any other stream is HTTP
            if (waitingForInitialStream) {
                waitingForInitialStream = false;

                ch.pipeline().addLast(new McProxyFrontendHandler(tcpHost, tcpPort));

                // They have connected, end the connect-timeout
                ch.parent().parent().pipeline().remove(ReadTimeoutHandler.class);
            } else {
                ch.pipeline().addLast(new TcpProxyFrontendHandler(tcpHost, httpPort));
            }
        }
    }

    private static class TcpProxyFrontendHandler extends ProxyHandler {

        private final int tcpPort;
        private final String tcpHost;

        public TcpProxyFrontendHandler(String tcpHost, int tcpPort) {
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
                        tcpChannelFuture.cause().printStackTrace();
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

    private static class McProxyFrontendHandler extends TcpProxyFrontendHandler {

        public McProxyFrontendHandler(String tcpHost, int tcpPort) {
            super(tcpHost, tcpPort);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            // Once our main QUIC stream is closed, close the whole QUIC channel
            if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
                ((QuicChannel) ctx.channel().parent()).close(true, 0, Unpooled.EMPTY_BUFFER);
            }
        }
    }

    public static void main(String[] args) throws IOException, CertificateException {
        QuicServerConnector connector = new QuicServerConnector();
        try {
            DataOutputStream out = new DataOutputStream(stdOut);
            DataInputStream in = new DataInputStream(System.in);

            while (in.read() == 0) {
                String host = in.readUTF();
                int tcpPort = in.readUnsignedShort();
                int httpPort = in.readUnsignedShort();
                int icePort = connector.bindProxy(host, tcpPort, httpPort);
                out.writeShort(icePort);
                out.flush();
            }
        } finally {
            connector.group.shutdownGracefully();
        }
    }
}
