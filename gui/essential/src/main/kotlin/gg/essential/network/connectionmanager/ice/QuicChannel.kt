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

import gg.essential.quic.backend.QuicBackendLoader
import gg.essential.quic.backend.QuicListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.slf4j.Logger

class QuicChannel(
    coroutineScope: CoroutineScope,
    private val logger: Logger,
    private val transportRecv: ReceiveChannel<ByteArray>,
    private val transportSend: SendChannel<ByteArray>,
) : QuicListener {
    private val internalScope = coroutineScope + Job(coroutineScope.coroutineContext.job) + Dispatchers.Default
    private val impl = QuicBackendLoader.INSTANCE.createImpl(logger, this)

    private val quicOpen = CompletableDeferred<Unit>()
    private val quicStreamInboundChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val quicStreamOutboundChannel = Channel<ByteArray>(Channel.UNLIMITED)

    init {
        internalScope.launch(CoroutineName("QUIC Write"), start = CoroutineStart.UNDISPATCHED) {
            try {
                quicOpen.await()
                for (buf in quicStreamOutboundChannel) {
                    impl.quicSend(buf)
                }

                logger.debug("QUIC outbound closed")
            } finally {
                impl.close()
            }
        }
    }

    private val feeder = internalScope.launch(CoroutineName("QUIC Feed"), start = CoroutineStart.LAZY) {
        for (buf in transportRecv) {
            impl.transportRecv(buf)
        }
    }

    suspend fun connect(): Pair<Pair<ReceiveChannel<ByteArray>, SendChannel<ByteArray>>, Int> {
        logger.debug("QuicChannel.connect")
        val httpPort = impl.connect()
        feeder.start()
        quicOpen.await()
        return Pair(quicStreamInboundChannel, quicStreamOutboundChannel) to httpPort
    }

    suspend fun accept(httpPort: Int): Pair<ReceiveChannel<ByteArray>, SendChannel<ByteArray>> {
        logger.debug("QuicChannel.accept")
        impl.accept(httpPort)
        feeder.start()
        quicOpen.await()
        return Pair(quicStreamInboundChannel, quicStreamOutboundChannel)
    }

    override fun onOpen() {
        logger.debug("QuicChannel.onOpen")
        quicOpen.complete(Unit)
    }

    override fun onReceivingStreamClosed() {
        logger.debug("QuicChannel.onReceivingStreamClosed")
        quicStreamInboundChannel.close()
    }

    override fun onClosed() {
        logger.debug("QuicChannel.onClosed")
        internalScope.cancel()
    }

    override fun transportSend(packet: ByteArray) {
        transportSend.trySend(packet)
    }

    override fun quicRecv(packet: ByteArray) {
        quicStreamInboundChannel.trySend(packet)
    }
}
