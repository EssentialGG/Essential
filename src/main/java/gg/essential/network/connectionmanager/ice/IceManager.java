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
package gg.essential.network.connectionmanager.ice;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import gg.essential.connectionmanager.common.packet.ice.IceCandidatePacket;
import gg.essential.connectionmanager.common.packet.ice.IceSessionPacket;
import gg.essential.gui.modals.FirewallBlockingModal;
import gg.essential.mixins.impl.feature.ice.common.AgentExt;
import gg.essential.mixins.impl.feature.ice.common.MergingDatagramSocketExt;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.ice.handler.IceCandidatePacketHandler;
import gg.essential.network.connectionmanager.ice.handler.IceSessionPacketHandler;
import gg.essential.network.connectionmanager.ice.netty.CloseAfterFirstMessage;
import gg.essential.network.connectionmanager.ice.netty.PseudoTcpChannelInitializer;
import gg.essential.network.connectionmanager.ice.netty.QuicStreamChannelInitializer;
import gg.essential.network.connectionmanager.sps.SPSConnectionTelemetry;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.quic.LogOnce;
import gg.essential.sps.FirewallUtil;
import gg.essential.sps.ResourcePackSharingHttpServer;
import gg.essential.sps.quic.QuicStream;
import gg.essential.sps.quic.jvm.ForkedJvmClientQuicStream;
import gg.essential.sps.quic.jvm.ForkedJvmServerQuicStreamPool;
import gg.essential.universal.UMinecraft;
import gg.essential.util.GuiUtil;
import gg.essential.util.Multithreading;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Pair;
import kotlin.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ice4j.StackProperties;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.KeepAliveStrategy;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.NominationStrategy;
import org.ice4j.ice.RemoteCandidate;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TrickleCallback;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.ice.harvest.UPNPHarvester;
import org.ice4j.pseudotcp.PseudoTcpSocket;
import org.ice4j.pseudotcp.PseudoTcpSocketFactory;
import org.ice4j.socket.MultiplexedDatagramSocket;
import org.ice4j.socket.MultiplexingDatagramSocket;
import org.ice4j.socket.SocketClosedException;
import org.ice4j.stack.StunClientTransaction;
import org.jetbrains.annotations.NotNull;
import org.jitsi.utils.logging2.LoggerImpl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static gg.essential.mixins.ext.network.NetworkSystemExtKt.getIceEndpoint;
import static gg.essential.network.connectionmanager.ice.util.CandidateUtil.candidateFromString;
import static gg.essential.network.connectionmanager.ice.util.CandidateUtil.candidateToString;
import static gg.essential.util.ExtensionsKt.getExecutor;
import static gg.essential.util.ExtensionsKt.logExceptions;

//#if FORGE && MC>=11200
//#if MC>=11700
//$$ import net.minecraftforge.fml.util.thread.SidedThreadGroups;
//#else
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
//#endif
//#endif

public class IceManager implements NetworkedManager {

    private static final long ICE_TIMEOUT = 30 /* sec */;
    private static final int TCP_TIMEOUT = 10 /* sec */ * 1000;

    /**
     * Chosen such that it does not conflict with STUN nor TURN nor QUIC nor PseudoTCP.
     *
     * As per the NEW TEXT in RFC7983, STUN and TURN can be identified by the value of the first byte being 0..3 and
     * 64..79 respectively.
     * As per section 17.3.1 of RFC9000, a QUIC Version 1 packet that has the first bit set to 0 must have the second
     * bit set to 1. As such, values in the range 0..63 can never be valid QUICv1 packets.
     * As per implementation, the first four bytes of a PseudoTCP packet are the conversation id which is 0 by default.
     *
     * As such, we should be safe to choose any value from 4..64. We chose 16 because according to RFC7983 that's
     * allocated to ZRTP, which we don't use, so it should most definitely be free.
     */
    private static final byte VOICE_HEADER_BYTE = 16;

    private static final Logger LOGGER = LogManager.getLogger(IceManager.class);

    public static final String[] STUN_HOSTS = getCommaSeparatedStrings("essential.sps.stun_hosts", new String[]{
        "us.stun.essential.gg",
        "eu.stun.essential.gg"
    });

    public static final String[] TURN_HOSTS = getCommaSeparatedStrings("essential.sps.turn_hosts", new String[]{
        "us.turn.essential.gg",
        "eu.turn.essential.gg"
    });

    private static final boolean SUPPORTS_QUIC;
    static {
        String property = System.getProperty("essential.sps.quic");
        if (property != null) {
            SUPPORTS_QUIC = Boolean.parseBoolean(property);
            LOGGER.info("Explicitly {} QUIC for SPS.", SUPPORTS_QUIC ? "enabled" : "disabled");
        } else {
            String arch = System.getProperty("os.arch");
            SUPPORTS_QUIC = "amd64".equals(arch) || UMinecraft.isRunningOnMac && ("aarch64".equals(arch) || "x86_64".equals(arch));
            if (!SUPPORTS_QUIC) {
                LOGGER.warn("Disabling QUIC for SPS because OS architecure ({}) is unsupported. " +
                    "This may result in slow connections under certain circumstances. " +
                    "Try reducing the server render distance in these cases.", arch);
            }
        }
    }

    private static EventLoopGroup makeIceEventLoopGroup(boolean server) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            // Note: For Forge 1.8 this needs to start with "Netty Server IO" (see FMLCommonHandler.getEffectiveSide)
            .setNameFormat("Netty " + (server ? "Server" : "Client") + " IO ICE #%d")
            .setDaemon(true)
            //#if FORGE && MC>=11200
            .setThreadFactory(server ? SidedThreadGroups.SERVER : SidedThreadGroups.CLIENT)
            //#endif
            .build();
        //#if MC>=11200
        return new io.netty.channel.DefaultEventLoopGroup(0, threadFactory);
        //#else
        //$$ return new io.netty.channel.local.LocalEventLoopGroup(0, threadFactory);
        //#endif
    }

    public static final Lazy<EventLoopGroup> ICE_SERVER_EVENT_LOOP_GROUP = LazyKt.lazy(() -> makeIceEventLoopGroup(true));
    public static final Lazy<EventLoopGroup> ICE_CLIENT_EVENT_LOOP_GROUP = LazyKt.lazy(() -> makeIceEventLoopGroup(false));
    private static final ForkedJvmServerQuicStreamPool QUIC_SERVER_POOL = new ForkedJvmServerQuicStreamPool();

    private static void bypassSocketLimit() {
        // By default the JVM has a rather strict limit of 25 DatagramSockets (regardless of whether they are bound!)
        // if a SecurityManager is installed, and Forge has a security manager installed.
        // This can quickly become a problem because Ice4J internally creates fake DatagramSockets and as such can
        // fairly quickly hit the limit (4 local addresses will do it with just two people connecting at the same time).
        // We cannot increase the limit (too late, it's only read from JVM properties once), but we can just reduce the
        // socket counter into the negative, so we'll do that.
        if (System.getSecurityManager() == null) {
            LOGGER.debug("No security manager installed, no need to bypass datagram socket limit.");
            return;
        }
        try {
            Class<?> resourceManagerClass = Class.forName("sun.net.ResourceManager");
            Field numSocketsField = resourceManagerClass.getDeclaredField("numSockets");
            numSocketsField.setAccessible(true);
            AtomicInteger numSockets = (AtomicInteger) numSocketsField.get(null);
            numSockets.addAndGet(-1000);
        } catch (Throwable t) {
            LOGGER.warn("Failed to bypass datagram socket limit:", t);
        }
    }

    static {
        bypassSocketLimit();

        // Jitsi has their own little logging library which forwards to java.util.logging by default.
        // We want to use Log4j though, so we make it create fake JUL loggers which in turn forward to Log4j.
        try {
            Function<String, java.util.logging.Logger> loggerFactory = Log4jAsJulLogger::new;

            Field loggerFactoryField = LoggerImpl.class.getDeclaredField("loggerFactory");
            loggerFactoryField.setAccessible(true);
            loggerFactoryField.set(null, loggerFactory);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // For some reason, the StunCandidateHarvester sets unreasonable defaults for these if they are not
        // set explicitly
        if (System.getProperty(StackProperties.MAX_CTRAN_RETRANS_TIMER) == null) {
            System.setProperty(StackProperties.MAX_CTRAN_RETRANS_TIMER, String.valueOf(StunClientTransaction.DEFAULT_MAX_WAIT_INTERVAL));
        }
        if (System.getProperty(StackProperties.MAX_CTRAN_RETRANSMISSIONS) == null) {
            System.setProperty(StackProperties.MAX_CTRAN_RETRANSMISSIONS, String.valueOf(StunClientTransaction.DEFAULT_MAX_RETRANSMISSIONS));
        }
    }

    @NotNull
    private final ConnectionManager connectionManager;

    @NotNull
    private final Map<UUID, IceConnection> connections = new ConcurrentHashMap<>();

    private int integratedServerVoicePort;

    /**
     * The port on which the proxy accepts http connections.
     * {@code null} if http proxying is not supported by the current connection.
     */
    private Integer proxyHttpPort;

    public IceManager(@NotNull ConnectionManager connectionManager, @NotNull SPSManager spsManager) {
        this.connectionManager = connectionManager;

        connectionManager.registerPacketHandler(IceSessionPacket.class, new IceSessionPacketHandler(this, spsManager));
        connectionManager.registerPacketHandler(IceCandidatePacket.class, new IceCandidatePacketHandler(this));
    }

    public IceConnection getConnection(UUID remote) {
        return this.connections.get(remote);
    }

    public void setVoicePort(int voicePort) {
        this.integratedServerVoicePort = voicePort;
    }

    public Integer getProxyHttpPort() {
        return this.proxyHttpPort;
    }

    private Agent createAgent(UUID user, boolean client) {
        // the "-q" in the ufrag is used to communicate to the other side that QUIC is supported (and preferred)
        // see [IceConnection.isQuic]
        String flags = SUPPORTS_QUIC ? "q" : "";
        // the "v12345" in the ufrag is used to communicate to the other side the port used by third-party voice mods
        // see [IceConnection.getVoicePort]
        if (!client) flags += "v" + integratedServerVoicePort;

        Agent agent = new Agent("essential-" + flags + "-", new LoggerImpl("ice4j-" + user));

        agent.setTrickling(true); // we can support this
        agent.setControlling(client); // spec mandates that the initiating side (in our case the client) is controlling

        if (!Boolean.getBoolean("essential.sps.legacy-nomination")) {
            // Disable the builtin nomination strategy and use our best RTT one instead
            agent.setNominationStrategy(NominationStrategy.NONE);
            agent.addStateChangeListener(new NominateBestRTT());
        } else {
            agent.setNominationStrategy(NominationStrategy.NOMINATE_FIRST_HOST_OR_REFLEXIVE_VALID);
        }

        for (String stunHost : STUN_HOSTS) {
            agent.addCandidateHarvester(new StunCandidateHarvester(new TransportAddress(stunHost, 3478, Transport.UDP)));
        }

        agent.addCandidateHarvester(new UPNPHarvester());
        for (String turnHost : TURN_HOSTS) {
            agent.addCandidateHarvester(new TurnCandidateHarvester(new TransportAddress(turnHost, 3478, Transport.UDP)));
        }
        return agent;
    }

    private Component createComponent(Agent agent, IceMediaStream mediaStream) {
        // While this does IO, it's only binding to local ports, so it should be fine to do on the main thread.
        // In fact, the docs on [CandidateHarvester.isHostHarvester] explicitly state that it should be non-blocking.
        try {
            return agent.createComponent(
                mediaStream,
                KeepAliveStrategy.SELECTED_ONLY, // good enough
                true // yes, we want to use this
            );
        } catch (IOException e) {
            LOGGER.error("Failed to create component:", e);
            return null;
        }
    }

    private PseudoTcpSocket createPseudoTcpSocket(DatagramSocket datagramSocket) throws IOException {
        try {
            return new PseudoTcpSocketFactory().createSocket(datagramSocket);
        } catch (SocketException e) {
            // This shouldn't really happen unless the security manager is weird cause it's a pseudo socket.
            LOGGER.error("Failed to create ICE pseudo tcp socket:", e);
            throw new IOException("ICE setup failed. Contact support.");
        }
    }

    private IceConnection setupAgent(UUID user, boolean client, Consumer<IceMediaStream> configureMediaStream) {
        Agent agent = createAgent(user, client);

        IceMediaStream mediaStream = agent.createMediaStream("minecraft");
        configureMediaStream.accept(mediaStream);

        Component component = createComponent(agent, mediaStream);
        if (component == null) {
            // This can fail if we cannot bind to any ports for some reason, not much we can do in this case.
            freeSafely(agent);
            return null;
        }

        IceConnection connection = new IceConnection(client, agent, mediaStream, component);
        IceConnection oldConnection = this.connections.put(user, connection);
        if (oldConnection != null) {
            freeSafely(oldConnection.agent);
        }

        this.connectionManager.send(new IceSessionPacket(user, agent.getLocalUfrag(), agent.getLocalPassword()));

        Multithreading.getScheduledPool().execute(() -> {
            TrickleCallback trickleCallback = candidates -> {
                if (candidates == null) {
                    this.connectionManager.send(new IceCandidatePacket(user, null));
                    return;
                }
                for (LocalCandidate candidate : candidates) {
                    String str = candidateToString(candidate);
                    LOGGER.debug("New local candidate for {}: {}", user, str);
                    this.connectionManager.send(new IceCandidatePacket(user, str));
                }
            };
            trickleCallback.onIceCandidates(component.getLocalCandidates());
            agent.startCandidateTrickle(trickleCallback);
        });

        return connection;
    }

    // Called from dedicated thread. Must be thread-safe and may block.
    public SocketAddress createClientAgent(UUID user) throws IOException {
        LOGGER.debug("Creating client-side ICE agent for {}", user);

        // If firewall is enabled, block the thread until the user disables it or cancels the modal
        while (FirewallUtil.INSTANCE.isFirewallBlocking()) {
            CompletableFuture<Boolean> retry = new CompletableFuture<>();

            // Firewall still enabled, prompt the user
            GuiUtil.INSTANCE.pushModal((manager) -> new FirewallBlockingModal(manager, user, (_manager) -> {
                retry.complete(false);
                return Unit.INSTANCE;
            }, (_manager) -> {
                retry.complete(true);
                return Unit.INSTANCE;
            }));

            // Modal closed without disabling firewall
            if (!retry.join()) {
                throw new IOException("ICE setup failed - Firewall enabled");
            }
        }

        IceConnection connection = setupAgent(user, true, mediaStream -> {
        });
        if (connection == null) {
            // This shouldn't really happen normally. Something fairly fundamental has gone wrong.
            throw new IOException("ICE setup failed. Contact support.");
        }

        // Wait for the ICE negotiation to complete
        // This necessitates that the remote side replies with its credentials cause we cannot actually start
        // connectivity until [IceConnection.setRemoteCredentials].
        try {
            connection.iceReadyFuture.join();
        } catch (CompletionException e) {
            // Unpack these for nicer error messages on the "Failed to connect" screen
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw e;
            }
        }

        // Once we have selected a remote address, we can try to connect to the server
        ChannelInitializer<LocalChannel> channelInitializer;
        try {
            TransportAddress remoteAddress = connection.getSelectedRemoteAddress();
            MultiplexingDatagramSocket iceSocket = connection.component.getSocket();
            iceSocket.connect(remoteAddress.getAddress(), remoteAddress.getPort());
            setupVoiceMultiplexing(iceSocket, false, connection.getVoicePort());
            if (connection.isQuic()) {
                ForkedJvmClientQuicStream quicStream = new ForkedJvmClientQuicStream(iceSocket);
                channelInitializer = new QuicStreamChannelInitializer(quicStream, user);
                proxyHttpPort = quicStream.getHttpPort();
            } else {
                PseudoTcpSocket tcpSocket = createPseudoTcpSocket(iceSocket);
                tcpSocket.connect(remoteAddress, TCP_TIMEOUT);
                channelInitializer = new PseudoTcpChannelInitializer(tcpSocket, user);
                proxyHttpPort = null;
            }
        } catch (IOException e) {
            freeSafely(connection.agent);
            throw e;
        }

        // And once that's done, spin up a single-use internal proxy server which MC can connect to.
        return new ServerBootstrap()
            .channel(LocalServerChannel.class)
            .handler(new CloseAfterFirstMessage())
            .childHandler(channelInitializer)
            .group(ICE_CLIENT_EVENT_LOOP_GROUP.getValue())
            .localAddress(LocalAddress.ANY)
            .bind()
            .syncUninterruptibly()
            .channel()
            .localAddress();
    }

    public void createServerAgent(UUID user, String ufrag, String password) {
        LOGGER.debug("Creating server-side ICE agent at request from {} (ufrag: {}, pwd: {})", user, ufrag, password);

        IceConnection connection = setupAgent(user, false, mediaStream -> {
            mediaStream.setRemoteUfrag(ufrag);
            mediaStream.setRemotePassword(password);
        });
        if (connection == null) {
            // This shouldn't really happen normally. Something fairly fundamental has gone wrong.
            return;
        }

        IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
        if (server == null) {
            LOGGER.error("Tried to register ICE socket but server was not running!");
            freeSafely(connection.agent);
            return;
        }

        // All set up, start connectivity checks
        connection.startConnectivityChecks();
        // and wait for them to complete
        logExceptions(connection.iceReadyFuture.thenComposeAsync(__ -> {
            CompletableFuture<ChannelInitializer<LocalChannel>> future = new CompletableFuture<>();
            try {
                // Setup connection telemetry
                CandidatePair selectedPair = connection.component.getSelectedPair();
                boolean relayed = selectedPair.getLocalCandidate().getType() == CandidateType.RELAYED_CANDIDATE
                    || selectedPair.getRemoteCandidate().getType() == CandidateType.RELAYED_CANDIDATE;
                ((MergingDatagramSocketExt) connection.component.getComponentSocket())
                    .essential$setConnectionTelemetry(new SPSConnectionTelemetry(user, connectionManager.getSpsManager().getSessionId(), relayed));

                // once ICE is done, try to accept the pseudo tcp connection
                TransportAddress remoteAddress = connection.getSelectedRemoteAddress();
                MultiplexingDatagramSocket iceSocket = connection.component.getSocket();
                iceSocket.connect(remoteAddress.getAddress(), remoteAddress.getPort());
                setupVoiceMultiplexing(iceSocket, true, integratedServerVoicePort);
                int httpPort = 9;
                Integer maybeHttpPort = ResourcePackSharingHttpServer.INSTANCE.getPort();
                if (maybeHttpPort != null) {
                    httpPort = maybeHttpPort;
                }
                if (connection.isQuic()) {
                    QuicStream quicStream = QUIC_SERVER_POOL.accept(iceSocket, httpPort);
                    future.complete(new QuicStreamChannelInitializer(quicStream, user));
                } else {
                    PseudoTcpSocket tcpSocket = createPseudoTcpSocket(iceSocket);
                    tcpSocket.accept(remoteAddress, TCP_TIMEOUT);
                    future.complete(new PseudoTcpChannelInitializer(tcpSocket, user));
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        }, ICE_SERVER_EVENT_LOOP_GROUP.getValue()).thenAcceptAsync(channelInitializer -> {
            // and once we've got a TCP connection, connect the proxy to the MC server (much like a local connection)
            SocketAddress iceEndpoint = getIceEndpoint(server.getNetworkSystem());
            new Bootstrap()
                .group(ICE_SERVER_EVENT_LOOP_GROUP.getValue())
                .handler(channelInitializer)
                .channel(LocalChannel.class)
                .connect(iceEndpoint);
        }, getExecutor(server)));
    }

    private void setupVoiceMultiplexing(MultiplexingDatagramSocket iceSocket, boolean server, int voicePort) throws SocketException {
        DatagramSocket realVoiceSocket;
        AtomicReference<SocketAddress> remoteAddress;
        if (server) {
            // We act as the client, so grab a random port to bind to and set the remote to the server
            realVoiceSocket = new DatagramSocket(0);
            remoteAddress = new AtomicReference<>(new InetSocketAddress(InetAddress.getLoopbackAddress(), voicePort));
        } else {
            // We act as the server, so grab the server port; we don't yet know the remote port
            try {
                realVoiceSocket = new DatagramSocket(voicePort);
            } catch (SocketException e) {
                LOGGER.error("Failed to allocate port for voice chat forwarding:", e);
                return;
            }
            remoteAddress = new AtomicReference<>(null);
        }

        LogOnce debugOnce = LogOnce.to(LOGGER::debug);

        MultiplexedDatagramSocket iceVoiceSocket = iceSocket.getSocket(datagramPacket -> {
            byte[] data = datagramPacket.getData();
            return data.length >= 1 && data[0] == VOICE_HEADER_BYTE;
        });

        Thread inboundThread = new Thread(() -> {
            // We want the voice socket to be closed when the session ends, otherwise the port will remain in use
            // until the socket object is garbage collected, potentially causing issues for future sessions.
            try(DatagramSocket _realVoiceSocket = realVoiceSocket) {
                while (!iceVoiceSocket.isClosed() && !realVoiceSocket.isClosed()) {
                    byte[] buf = new byte[0xffff];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    iceVoiceSocket.receive(packet);

                    debugOnce.log("inbound", packet.getLength());

                    // Remove our header
                    packet.setData(buf, 1, packet.getLength() - 1);

                    packet.setSocketAddress(remoteAddress.get());
                    realVoiceSocket.send(packet);
                }
            } catch (IOException e) {
                debugOnce.log("inboundException", e);
                if (!iceVoiceSocket.isClosed() && !realVoiceSocket.isClosed() && !(e instanceof PortUnreachableException || e instanceof SocketClosedException)) {
                    e.printStackTrace();
                }
            }
        }, "ice voice inbound");
        inboundThread.setDaemon(true);
        inboundThread.start();

        Thread outboundThread = new Thread(() -> {
            try {
                while (!iceVoiceSocket.isClosed() && !realVoiceSocket.isClosed()) {
                    byte[] buf = new byte[0xffff];
                    DatagramPacket packet = new DatagramPacket(buf, 1, buf.length - 1); // reserve space for our header
                    realVoiceSocket.receive(packet);

                    debugOnce.log("outbound", packet.getLength());

                    if (!server) {
                        // The client initiates communication with the server, so we need to store the client address
                        // once we receive the first packet from it, we cannot know the address beforehand.
                        // The address may also change if the client decides to close and re-open its datagram socket.
                        remoteAddress.set(packet.getSocketAddress());
                    }

                    // Add our header
                    buf[0] = VOICE_HEADER_BYTE;
                    packet.setData(buf, 0, packet.getLength() + 1);

                    packet.setSocketAddress(iceVoiceSocket.getRemoteSocketAddress());
                    iceVoiceSocket.send(packet);
                }
            } catch (IOException e) {
                debugOnce.log("outboundException", e);
                if (!iceVoiceSocket.isClosed() && !realVoiceSocket.isClosed() && !(e instanceof PortUnreachableException || e instanceof SocketClosedException)) {
                    e.printStackTrace();
                }
            }
        }, "ice voice outbound");
        outboundThread.setDaemon(true);
        outboundThread.start();
    }

    public void addRemoteCandidate(UUID user, String candidateStr) {
        LOGGER.debug("New remote candidate from {}: {}", user, candidateStr);

        IceConnection connection = this.connections.get(user);
        if (connection == null) {
            LOGGER.debug("Ignoring candidate from {} because they have no active session.", user);
            return;
        }

        if (candidateStr == null) {
            ((AgentExt) connection.agent).setRemoteTricklingDone();
            return;
        }

        RemoteCandidate candidate = candidateFromString(candidateStr, connection.component);
        if (candidate == null) {
            return;
        }

        // Running these two async because they synchronize on internal locks and we do not want to block the main thread
        Multithreading.runAsync(() -> {
            connection.component.addUpdateRemoteCandidates(candidate);
            connection.component.updateRemoteCandidates();
        });
    }

    private static String[] getCommaSeparatedStrings(String property, String[] defaults) {
        String str = System.getProperty(property);
        return str != null ? str.split(",") : defaults;
    }

    // agent.free has questionable thread safety, and we don't want an error in freeing it to be fatal
    private static void freeSafely(Agent agent) {
        try {
            agent.free();
        } catch (Exception e) {
            // We do want it to be fatal for integration tests though, hence the ERROR level here
            LOGGER.error("Error while freeing ICE agent:", e);
        }
    }

    public static class IceConnection {
        private final Agent agent;
        private final IceMediaStream mediaStream;
        private final Component component;
        private final CompletableFuture<Void> iceReadyFuture;

        private boolean needsRemoteCredentials;

        public IceConnection(boolean client, Agent agent, IceMediaStream mediaStream, Component component) {
            this.agent = agent;
            this.mediaStream = mediaStream;
            this.component = component;
            this.iceReadyFuture = createReadyFuture(agent, client);

            // The client is the initiating side in our setup, so it must wait for the remote side to reply
            // with its credentials. Whereas the server only sets up in response to the client request, so it
            // immediately has all the credentials available.
            this.needsRemoteCredentials = client;
        }

        public void setRemoteCredentials(String ufrag, String password) {
            if (!this.needsRemoteCredentials) {
                return;
            }
            this.needsRemoteCredentials = false;

            this.mediaStream.setRemoteUfrag(ufrag);
            this.mediaStream.setRemotePassword(password);

            // Once we have credentials, we can start doing connectivity checks
            this.startConnectivityChecks();
        }

        public void startConnectivityChecks() {
            // Blocking method, so we run it on the dynamic pool
            Multithreading.getScheduledPool().execute(this.agent::startConnectivityEstablishment);
        }

        public TransportAddress getSelectedRemoteAddress() {
            CandidatePair selectedPair = this.component.getSelectedPair();
            if (selectedPair == null) {
                throw new IllegalStateException("No candidate pair selected");
            }
            return selectedPair.getRemoteCandidate().getTransportAddress();
        }

        private static CompletableFuture<Void> createReadyFuture(Agent agent, boolean client) {
            CompletableFuture<Void> future = new CompletableFuture<>();

            // We only want to wait so long (the agent itself doesn't necessarily fail if it never fully starts)
            Multithreading.scheduleOnBackgroundThread(() -> future.completeExceptionally(new IceFailedException(client)), ICE_TIMEOUT, TimeUnit.SECONDS);

            Runnable update = () -> {
                IceProcessingState state = agent.getState();
                if (state.isEstablished()) {
                    // Done and established? We're good.
                    future.complete(null);
                } else if (state.isOver()) {
                    // Done but not established. Oh no, bad news.
                    future.completeExceptionally(new IceFailedException(client));
                }
            };
            // Listen for changes
            agent.addStateChangeListener(event -> update.run());
            // but also check the current state in case we're already done by now
            update.run();

            // If we fail to connect, always clean up
            future.exceptionally(__ -> {
                freeSafely(agent);
                return null;
            });

            return future;
        }

        /**
         * We use the ufrag to communicate alphanumeric flags between both sides.
         * This allows us to switch to an improved implementation if both sides support it without requiring infra
         * changes to communicate that.
         * The ufrag will generally follow the format "essential-${flags}-${random}". The flags part may be missing from
         * old clients.
         */
        private Pair<String, String> getUfragFlags() {
            String localUfrag = this.agent.getLocalUfrag();
            String remoteUfrag = this.mediaStream.getRemoteUfrag();
            if (localUfrag == null || remoteUfrag == null) {
                LOGGER.warn("Ufrag missing?!");
                LOGGER.warn("localUfrag: {}", localUfrag);
                LOGGER.warn("remoteUfrag: {}", remoteUfrag);
                return new Pair<>("", "");
            }
            String[] localParts = localUfrag.split("-");
            String[] remoteParts = remoteUfrag.split("-");
            String localFlags = localParts.length > 2 ? localParts[1] : "";
            String remoteFlags = remoteParts.length > 2 ? remoteParts[1] : "";
            return new Pair<>(localFlags, remoteFlags);
        }

        /** Whether this connection should use QUIC rather than Ice4j's PseudoTcpSocket. */
        public boolean isQuic() {
            // We communicate support for QUIC via the ufrag. If both sides support it, use it.
            Pair<String, String> flags = this.getUfragFlags();
            boolean localSupport = flags.getFirst().contains("q");
            boolean remoteSupport = flags.getSecond().contains("q");
            if (localSupport && remoteSupport) {
                LOGGER.info("Using QUIC because both parties support it.");
                return true;
            } else {
                String who;
                if (localSupport) {
                    who = "the remote client does";
                } else if (remoteSupport) {
                    who = "the local client does";
                } else {
                    who = "both sides do";
                }
                LOGGER.warn("Not using QUIC (falling back to PseudoTCP) because {} not support it.", who);
                return false;
            }
        }

        /** Returns the port used for voice on the server as signaled by the remote side. 0 if unknown. */
        public int getVoicePort() {
            String flags = getUfragFlags().getSecond();
            int offset = flags.indexOf('v');
            if (offset == -1) {
                LOGGER.warn("Remote does not support voice tunneling.");
                return 0;
            }
            int endOffset = offset + 1;
            while (endOffset < flags.length() && Character.isDigit(flags.charAt(endOffset))) {
                endOffset++;
            }
            try {
                return Integer.parseInt(flags.substring(offset + 1, endOffset));
            } catch (NumberFormatException e) {
                LOGGER.error("Failed to parse remote voice port from \"" + flags + "\":", e);
                return 0;
            }
        }
    }

    // MC uses toString to display the error on the "Failed to connect to server" screen
    private static class PrettyIOException extends IOException {
        public PrettyIOException(String message) {
            super(message);
        }

        @Override
        public String toString() {
            return this.getMessage();
        }
    }

    private static class IceFailedException extends PrettyIOException {
        public IceFailedException(boolean client) {
            super((client ? "Server" : "Client") + " is unreachable (ICE failed)");
        }
    }
}
