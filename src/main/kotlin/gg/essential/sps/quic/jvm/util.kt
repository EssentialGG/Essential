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
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.PortUnreachableException

// EM-1571: We hardcode this instead of using getLoopbackAddress due to issues with IPv6 loopback on some Windows systems
val LOCALHOST: InetAddress = InetAddress.getByName("127.0.0.1")

/**
 * Forwards any datagrams received from the [source] socket to the [target] socket.
 *
 * The [target] socket must be connected to a remote address.
 * If either socket is closed, the forwarding will terminate and the other socket will be closed as well.
 */
fun forwardAsync(name: String, source: DatagramSocket, target: DatagramSocket, logOnce: LogOnce) {
    val thread = Thread({ source.use { s -> target.use { t -> forward(name, s, t, logOnce) } } }, name)
    thread.isDaemon = true
    thread.start()
}

private fun forward(name: String, source: DatagramSocket, target: DatagramSocket, logOnce: LogOnce) {
    try {
        while (!source.isClosed && !target.isClosed) {
            val buf = ByteArray(0xffff)
            val packet = DatagramPacket(buf, buf.size)
            source.receive(packet)
            logOnce.log(name, packet.length)
            packet.socketAddress = target.remoteSocketAddress
            target.send(packet)
        }
    } catch (e: IOException) {
        logOnce.log("$name exception", e)
        if (!source.isClosed && !target.isClosed && e !is PortUnreachableException) {
            e.printStackTrace()
        }
    }
}
