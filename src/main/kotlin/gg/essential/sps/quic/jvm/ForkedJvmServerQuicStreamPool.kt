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
package gg.essential.sps.quic.jvm

import gg.essential.quic.LogOnce
import gg.essential.sps.quic.QuicStream
import gg.essential.util.Multithreading
import gg.essential.util.RefCounted
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * A container for one or more server-side QUIC streams.
 *
 * The underlying connector is booted on demand when the first stream is to be created, and freed when all are closed.
 */
class ForkedJvmServerQuicStreamPool {

    private val lazyConnector = LazyConnector()

    /**
     * Binds a new QUIC proxy which connects the application layer to the given tcp port.
     * Returns the port on which the QUIC proxy expects transport layer packets.
     *
     * The application layer connection is only opened once the QUIC channel+stream have been connected.
     */
    @Synchronized
    private fun ForkedJvmQuicConnector.bind(tcpPort: Int, httpPort: Int): Int {
        output.write(0)
        output.writeUTF(LOCALHOST.hostAddress)
        output.writeShort(tcpPort)
        output.writeShort(httpPort)
        output.flush()
        return input.readUnsignedShort()
    }

    /**
     * Wraps the given ICE socket into a QuicStream by listening for an incoming connection from the remote side.
     */
    fun accept(iceSocket: DatagramSocket, httpPort: Int): QuicStream {
        val connector = lazyConnector.obtain()
        val udpSocket = DatagramSocket(0, LOCALHOST)
        try {
            ServerSocket(0, 1, LOCALHOST).use { serverSocket ->
                val connectorPort = connector.bind(serverSocket.localPort, httpPort)
                LOGGER.debug("udp: ${udpSocket.localPort} <> $connectorPort, tcp: ${serverSocket.localPort}, http: $httpPort")

                udpSocket.connect(LOCALHOST, connectorPort)
                forwardAsync("quic→ice", udpSocket, iceSocket, LogOnce.to(LOGGER::debug))
                forwardAsync("ice→quic", iceSocket, udpSocket, LogOnce.to(LOGGER::debug))

                serverSocket.soTimeout = Duration.of(10, ChronoUnit.SECONDS).toMillis().toInt()
                val tcpSocket = serverSocket.accept()
                return Stream(iceSocket, udpSocket, tcpSocket)
            }
        } catch (e: Exception) {
            IOUtils.closeQuietly(udpSocket)
            lazyConnector.release()
            throw e
        }
    }

    private class LazyConnector {
        private val inner = RefCounted<ForkedJvmQuicConnector>()

        fun obtain() = inner.obtain { ForkedJvmQuicConnector("gg.essential.quic.QuicServerConnector") }

        fun release() = inner.release { connector ->
            // We don't need to wait for this, so do it async.
            Multithreading.scheduledPool.execute { connector.close() }
        }
    }

    /**
     * A single server-side QUIC stream wrapping an ICE datagram socket.
     *
     * Closing the stream will also close the ICE datagram sockets.
     */
    private inner class Stream(
        /** Socket used to communicate with the remote side via ICE. */
        private val iceSocket: DatagramSocket,
        /** Unreliable socket connected to the QUIC connector. The transport layer from the POV of QUIC. */
        private val udpSocket: DatagramSocket,
        /** Reliable socket connected to the QUIC connector. The application layer from the POV of QUIC. */
        private val tcpSocket: Socket,
    ) : QuicStream {

        override val inputStream: InputStream = tcpSocket.getInputStream()
        override val outputStream: OutputStream = tcpSocket.getOutputStream()

        private var closed = false

        @Synchronized
        override fun close() {
            if (closed) return
            closed = true

            LOGGER.debug("close")

            // Close our end of the tcp stream, this should gracefully close the QUIC stream, so we do not yet want to
            // kill the UDP/ICE sockets
            tcpSocket.close()

            // Give it 5 seconds to close the connection
            Multithreading.scheduleOnBackgroundThread({
                // Enough time has passed, now we can close our sockets (which will shut down the UDP/ICE forwarding
                // threads) as well as release our connector (which will quit the JVM if no other connections remain)
                LOGGER.debug("force close")
                iceSocket.close()
                udpSocket.close()
                lazyConnector.release()
            }, 5, TimeUnit.SECONDS)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger()
    }
}