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
package gg.essential.network.connectionmanager.ice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.ice4j.pseudotcp.PseudoTCPBase
import org.ice4j.pseudotcp.PseudoTcpNotify
import org.ice4j.pseudotcp.PseudoTcpSocketFactory.DEFAULT_CONVERSATION_ID
import org.ice4j.pseudotcp.WriteResult
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class PseudoTcpChannel(
    coroutineScope: CoroutineScope,
    private val transportRecv: ReceiveChannel<ByteArray>,
    private val transportSend: SendChannel<ByteArray>,
) : PseudoTcpNotify {
    private val internalScope = coroutineScope + Job(coroutineScope.coroutineContext.job) + Dispatchers.Default.limitedParallelism(1)
    private val impl = PseudoTCPBase(this, DEFAULT_CONVERSATION_ID).apply {
        // Maximum segment size adjustment logic in PseudoTCPBase is broken; if we were to start with the default value
        // it'll hit a max-retransmissions limit and abort the connection before it ever reaches the correct MTU.
        // It also doesn't actively retransmit when a smaller amount is acked but rather waits 250ms to hit the regular
        // timeout before it retransmits, so we're effectively limited to like 6KB/s for the first few seconds too.
        // PseudoTcpSocketImpl also sets this in its constructor.
        notifyMTU(1450);
    }

    private val tcpOpen = CompletableDeferred<Unit>()
    private val tcpReadable = Channel<Unit>(Channel.CONFLATED)
    private val tcpWritable = Channel<Unit>(Channel.CONFLATED)
    private val tcpInboundChannel = Channel<ByteArray>()
    private val tcpOutboundChannel = Channel<ByteArray>()

    private var clockJob: Job? = null

    init {
        internalScope.launch(CoroutineName("PseudoTCP Read")) {
            tcpOpen.await()
            val buf = ByteArray(1024)
            while (true) {
                val len = try {
                    impl.recv(buf, buf.size)
                } catch (e: IOException) {
                    break // recv only throws when closed
                }
                scheduleClock()
                if (len == 0) {
                    tcpReadable.receive()
                    continue
                }
                val data = buf.sliceArray(0 until len)
                if (!data.contentEquals(CLOSE_PACKET)) {
                    tcpInboundChannel.send(data)
                } else {
                    tcpInboundChannel.close()
                    break
                }
            }
        }
        internalScope.launch(CoroutineName("PseudoTCP Write")) {
            for (buf in tcpOutboundChannel) {
                var offset = 0
                while (offset < buf.size) {
                    val sent = try {
                        impl.send(buf, offset, buf.size - offset)
                    } catch (e: IOException) {
                        break // send only throws when closed
                    }
                    if (sent == 0) {
                        scheduleClock()
                        tcpWritable.receive()
                        continue
                    }
                    offset += sent
                }
                scheduleClock()
            }
            // Flush before and after ensures that our close packet actually gets its own UDP packet
            flush()
            impl.send(CLOSE_PACKET, CLOSE_PACKET.size)
            scheduleClock()
            flush()
            onTcpClosed(impl, null)
        }
        internalScope.launch(CoroutineName("PseudoTcp Feed")) {
            for (buf in transportRecv) {
                impl.notifyPacket(buf, buf.size)
                scheduleClock()
            }
        }
    }

    private fun scheduleClock() {
        clockJob?.cancel()
        clockJob = null

        val delayMs = impl.getNextClock(PseudoTCPBase.now())
        if (delayMs == -1L) {
            return
        }

        clockJob = internalScope.launch(CoroutineName("PseudoTcp Clock")) {
            delay(delayMs)

            impl.notifyClock(PseudoTCPBase.now())

            scheduleClock()
        }
    }

    suspend fun connect(): Pair<ReceiveChannel<ByteArray>, SendChannel<ByteArray>> {
        impl.connect()
        scheduleClock()
        tcpOpen.await()
        return Pair(tcpInboundChannel, tcpOutboundChannel)
    }

    suspend fun accept(): Pair<ReceiveChannel<ByteArray>, SendChannel<ByteArray>> {
        scheduleClock()
        tcpOpen.await()
        return Pair(tcpInboundChannel, tcpOutboundChannel)
    }

    override fun onTcpOpen(tcp: PseudoTCPBase) {
        tcpOpen.complete(Unit)
    }

    override fun onTcpReadable(tcp: PseudoTCPBase) {
        tcpReadable.trySend(Unit)
    }

    override fun onTcpWriteable(tcp: PseudoTCPBase) {
        tcpWritable.trySend(Unit)
    }

    override fun onTcpClosed(tcp: PseudoTCPBase, e: IOException?) {
        internalScope.cancel()
        tcpInboundChannel.close(e)
        tcpOutboundChannel.close(e)
        if (e != null) {
            tcpOpen.completeExceptionally(e)
        }
    }

    override fun tcpWritePacket(tcp: PseudoTCPBase, buffer: ByteArray, len: Int): WriteResult {
        assert(buffer.size == len)
        transportSend.trySend(buffer)
        return WriteResult.WR_SUCCESS
    }

    private suspend fun flush() = withContext(Dispatchers.IO) {
        val ackNotify = impl.ackNotify
        synchronized(ackNotify) {
            val method = PseudoTCPBase::class.java.getDeclaredMethod("getBytesBufferedNotSent")
            method.isAccessible = true
            while (method.invoke(impl) as Long > 0) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (ackNotify as Object).wait(100)
            }
        }
    }

    companion object {
        // Ice4j's pseudo TCP implementation does not actually implement socket shutdown. So, to avoid having to wait
        // for timeout each time, we send a specially crafted packet which we can detect on the other side and then
        // close from there as well.
        private val CLOSE_PACKET: ByteArray = run {
            // Just need something unique but constant
            val uuid = UUID.nameUUIDFromBytes("Essential Close Packet".encodeToByteArray());
            val buf = ByteBuffer.wrap(ByteArray(16)).order(ByteOrder.BIG_ENDIAN)
            buf.putLong(uuid.leastSignificantBits);
            buf.putLong(uuid.mostSignificantBits);
            buf.array()
        }
    }
}