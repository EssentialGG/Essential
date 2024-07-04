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
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramSocket
import java.net.Socket

/**
 * A single client-side QUIC stream wrapping an ICE datagram socket.
 *
 * Closing the stream will also close the ICE datagram sockets.
 */
class ForkedJvmClientQuicStream(
    /** Socket used to communicate with the remote side via ICE. */
    private val iceSocket: DatagramSocket
) : QuicStream {
    private val connector = ForkedJvmQuicConnector("gg.essential.quic.QuicClientConnector")

    /** Unreliable socket connected to the QUIC connector. The transport layer from the POV of QUIC. */
    private val udpSocket: DatagramSocket = DatagramSocket(0, LOCALHOST)

    /** Reliable socket connected to the QUIC connector. The application layer from the POV of QUIC. */
    private val tcpSocket: Socket

    /**
     * Port on which the QUIC stream proxy accepts http connections.
     */
    val httpPort: Int

    /**
     * Binds a new QUIC proxy which accepts application layer connections and sends its transport layer packets to the
     * given port.
     * Returns the port on which the QUIC proxy expects transport layer packets, as well as the port on which it expects
     * the application layer connection.
     *
     * No packets are sent on the transport layer until the application layer connects to the returned port.
     */
    @Synchronized
    private fun ForkedJvmQuicConnector.bind(udpPort: Int): Triple<Int, Int, Int> {
        output.writeUTF(LOCALHOST.hostAddress)
        output.writeShort(udpPort)
        output.flush()
        return Triple(input.readUnsignedShort(), input.readUnsignedShort(), input.readUnsignedShort())
    }

    init {
        try {
            val (udpPort, tcpPort, httpPort) = connector.bind(udpSocket.localPort)
            LOGGER.debug("udp: ${udpSocket.localPort} <> $udpPort, tcp: $tcpPort, http: $httpPort")

            udpSocket.connect(LOCALHOST, udpPort)
            forwardAsync("quic→ice", udpSocket, iceSocket, LogOnce.to(LOGGER::debug))
            forwardAsync("ice→quic", iceSocket, udpSocket, LogOnce.to(LOGGER::debug))

            tcpSocket = Socket(LOCALHOST, tcpPort)

            this.httpPort = httpPort
        } catch (e: Exception) {
            IOUtils.closeQuietly(udpSocket)
            IOUtils.closeQuietly(connector)
            throw e
        }
    }

    override val inputStream: InputStream = tcpSocket.getInputStream()
    override val outputStream: OutputStream = tcpSocket.getOutputStream()

    @Synchronized
    override fun close() {
        LOGGER.debug("close")

        // Close our end of the tcp stream, this should gracefully close the QUIC stream, so we do not yet want to
        // kill the UDP/ICE sockets
        tcpSocket.close()

        // Gracefully shut down the connector
        connector.close()

        // Connector should be dead, now we can close our sockets (which will shut down the UDP/ICE forwarding threads)
        udpSocket.close()
        iceSocket.close()
    }

    companion object {
        private val LOGGER = LogManager.getLogger()
    }
}