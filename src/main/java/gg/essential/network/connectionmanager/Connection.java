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
package gg.essential.network.connectionmanager;

import com.google.common.primitives.Bytes;
import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.connection.ConnectionKeepAlivePacket;
import gg.essential.handlers.CertChain;
import gg.essential.network.connectionmanager.ConnectionManagerKt.CloseInfo;
import gg.essential.network.connectionmanager.legacyjre.LegacyJre;
import gg.essential.network.connectionmanager.legacyjre.LegacyJreDnsResolver;
import gg.essential.network.connectionmanager.legacyjre.LegacyJreSocketFactory;
import gg.essential.util.LimitedExecutor;
import gg.essential.util.Multithreading;
import kotlin.Lazy;
import kotlin.LazyKt;
import org.java_websocket.client.DnsResolver;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Connection extends WebSocketClient {

    private static final Lazy<Function<String, SSLSocketFactory>> SSL_SOCKET_FACTORY_FACTORY = LazyKt.lazy(() -> {
        try {
            SSLSocketFactory factory = new CertChain()
                .loadEmbedded()
                .done()
                .getFirst()
                .getSocketFactory();

            if (LegacyJre.IS_LEGACY_JRE_51 || LegacyJre.IS_LEGACY_JRE_74) {
                Essential.logger.info("Using LegacyJreSocketFactory");
                return host -> new LegacyJreSocketFactory(factory, host);
            } else {
                Essential.logger.info("Using Default JreSocketFactory");
                return host -> factory;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    private static final Lazy<DnsResolver> DNS_RESOLVER = LazyKt.lazy(() -> {
        if (LegacyJre.IS_LEGACY_JRE_51) {
            Essential.logger.info("Using LegacyJreDnsResolver");
            return new LegacyJreDnsResolver();
        } else {
            Essential.logger.info("Using Default JreDnsResolver");
            return uri -> InetAddress.getByName(uri.getHost());
        }
    });

    @NotNull
    private final Executor sendExecutor = new LimitedExecutor(Multithreading.getPool(), 1, new ConcurrentLinkedQueue<>());

    //
    @NotNull
    private final Callbacks callbacks;
    private final ConnectionCodec codec = new ConnectionCodec();

    private int usingProtocol = 1;
    private ScheduledFuture<?> timeoutTask;

    private static final int MAX_PROTOCOL = 6;

    public Connection(@NotNull Callbacks callbacks) {
        super(
            URI.create(
                System.getProperty(
                    "essential.cm.host",
                    System.getenv().getOrDefault("ESSENTIAL_CM_HOST", "wss://connect.essential.gg/v1")
                )
            )
        );

        this.callbacks = callbacks;

        this.setTcpNoDelay(true);
        this.setReuseAddr(true);
        this.setConnectionLostTimeout(0); // We have our own keep alive.
    }

    public void close(@NotNull final CloseReason closeReason) {
        this.close(closeReason.getCode(), closeReason.name());
    }

    @Override
    public void onOpen(@NotNull final ServerHandshake serverHandshake) {
        this.usingProtocol = Integer.parseInt(serverHandshake.getFieldValue("Essential-Protocol-Version"));

        scheduleTimeout();

        this.callbacks.onOpen();
    }

    @Override
    public void onClosing(int code, @NotNull String reason, boolean remote) {
        onClosingOrClosed(code, reason, remote);
    }

    @Override
    public void onClose(final int code, @NotNull final String reason, final boolean remote) {
        onClosingOrClosed(code, reason, remote);
    }

    private void onClosingOrClosed(int code, @NotNull String reason, boolean remote) {
        ScheduledFuture<?> timeoutTask = this.timeoutTask;
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            this.timeoutTask = null;
        }

        boolean outdated = reason.contains("Invalid status code received: 410") || reason.contains("Invalid status code received: 404");

        this.callbacks.onClose(new CloseInfo(code, reason, remote, outdated));
    }

    @Override
    public void onMessage(@NotNull final String message) {
        // Method is not support, I dislike the fact that this is a required method to have though.
    }

    // Debug is kept for the time being.
    @Override
    public void onMessage(@NotNull final ByteBuffer byteBuffer) {
        final Packet packet = codec.decode(byteBuffer.array());
        if (packet == null) {
            return;
        }
        this.onMessage(packet);
    }

    private void onMessage(final Packet packet) {
        if (packet instanceof ConnectionKeepAlivePacket) {
            scheduleTimeout();
            Packet response = new ConnectionKeepAlivePacket();
            response.setUniqueId(packet.getPacketUniqueId());
            this.send(response);
            return;
        }
        this.callbacks.onPacketAsync(packet);
    }

    @Override
    public void onError(@NotNull final Exception e) {
        Essential.logger.error("Critical error occurred on connection management. ", e);
    }

    /**
     * Send a packet to the Connection Manager.
     *
     * @param packet           to send to the connection manager
     */
    public void send(@NotNull final Packet packet) {
        final Packet fakeReplyPacket = packet.getFakeReplyPacket();
        if (fakeReplyPacket != null) {
            fakeReplyPacket.setUniqueId(packet.getPacketUniqueId());
            Multithreading.scheduleOnBackgroundThread(() -> onMessage(fakeReplyPacket), packet.getFakeReplyDelayMs(), TimeUnit.MILLISECONDS);
            return;
        }

        sendExecutor.execute(() -> doSend(packet));
    }

    private void doSend(Packet packet) {
        codec.encode(packet, this::send);
    }

    public void setupAndConnect(String userName, byte[] secret) {
        byte[] colon = ":".getBytes(StandardCharsets.UTF_8);
        byte[] name = userName.getBytes(StandardCharsets.UTF_8);
        byte[] nameSecret = Bytes.concat(name, colon, secret);
        String encoded = Base64.getEncoder().encodeToString(nameSecret);
        this.addHeader("Authorization", "Basic " + encoded);

        String protocolProperty = System.getProperty("essential.cm.protocolVersion");
        if (protocolProperty == null) {
            this.addHeader("Essential-Max-Protocol-Version", String.valueOf(MAX_PROTOCOL));
        } else {
            this.addHeader("Essential-Protocol-Version", protocolProperty);
        }

        // Attempt to connect.
        try {
            this.setDnsResolver(DNS_RESOLVER.getValue());
            if ("wss".equals(this.uri.getScheme())) {
                this.setSocketFactory(SSL_SOCKET_FACTORY_FACTORY.getValue().apply(this.uri.getHost()));
            }

            this.connect();
        } catch (final Exception e) {
            Essential.logger.error("Error when connecting to Essential ConnectionManager.", e);

            e.printStackTrace();
        }
    }

    private void scheduleTimeout() {
        ScheduledFuture<?> timeoutTask = this.timeoutTask;
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }

        this.timeoutTask = Multithreading.getScheduledPool().schedule(
            () -> this.close(CloseReason.SERVER_KEEP_ALIVE_TIMEOUT),
            60L, TimeUnit.SECONDS);
    }

    interface Callbacks {
        void onOpen();
        void onPacketAsync(Packet packet);
        void onClose(CloseInfo info);
    }
}
