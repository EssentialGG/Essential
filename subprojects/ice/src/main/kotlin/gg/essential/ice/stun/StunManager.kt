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
package gg.essential.ice.stun

import gg.essential.ice.stun.StunSocket.ReceivedPacket
import gg.essential.ice.toHexString
import gg.essential.slf4j.with
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.seconds

/**
 * This class implements STUN request and response management.
 * It allows multiple virtual STUN servers and clients to run on a single (or multiple) [StunSocket]s.
 */
class StunManager(private val scope: CoroutineScope) {
    val bindingPacer = BindingPacer(scope)

    private val activeRequests = mutableMapOf<TransactionId, StunRequest>()

    private val servers = mutableMapOf<String, StunServer>()
    private val remoteToServer = mutableMapOf<InetSocketAddress, StunServer>()

    fun sendRequest(request: StunRequest) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            activeRequests[request.request.transactionId] = request
            try {
                request.start().join()
            } finally {
                activeRequests.remove(request.request.transactionId)
            }
        }
    }

    fun registerServer(job: Job, ufrag: String, password: ByteArray): StunServer {
        check(ufrag !in servers)

        val server = StunServer(job, password)

        servers[ufrag] = server
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                job.join()
                // wait some extra time for in-flight packets (we don't want to warn about them being unexpected)
                delay(15.seconds)
            } finally {
                servers.remove(ufrag)
                server.knownRemotes.forEach { remoteToServer.remove(it, server) }
            }
        }

        return server
    }

    fun getServer(remoteAddress: InetSocketAddress): StunServer? {
        return remoteToServer[remoteAddress]
    }

    /**
     * Tries to decode and process the given packet as a STUN message.
     * If the message contained within was not handled by the StunManager, it will be returned and should be handled
     * by the caller.
     */
    fun messageReceived(parentLogger: Logger, packet: ReceivedPacket): StunMessage? {
        val message = try {
            StunMessage.decode(
                packet.data,
                { servers[it.substringBefore(":")]?.password },
                { activeRequests[it]?.request?.integrityProtectionKey },
            )
        } catch (e: Exception) {
            // Spec wants us to silently discard the packet on errors, but this isn't a public STUN server,
            // and I'd rather know when something unexpected happened (cause it's likely an issue with our
            // code).
            parentLogger.atWarn()
                .addKeyValue("remoteAddress", packet.source)
                .setCause(e)
                .log("Failed to decode STUN packet. Content: ${packet.data.toHexString()}")
            return null
        }

        val logger = parentLogger.with {
            addKeyValue("remoteAddress", packet.source)
            addKeyValue("tId", message.transactionId)
        }

        return when (message.cls) {
            StunClass.Request -> {
                if (!message.isIntegrityProtected) {
                    logger.warn("Ignoring STUN request because it was missing HMAC.")
                    return null
                }

                val ufrag = message.attribute<StunAttribute.Username>()?.username?.substringBefore(":")
                val server = servers[ufrag]
                if (server != null) {
                    if (server.knownRemotes.add(packet.source)) {
                        remoteToServer[packet.source] = server
                    }
                    server.stunRequestReceiveChannel.trySend(packet to message)
                    null
                } else {
                    message
                }
            }
            StunClass.Indication -> {
                when (message.type) {
                    StunType.Binding -> {
                        logger.trace("Got STUN Binding indication.")
                        null
                    }
                    else -> message
                }
            }
            StunClass.ResponseSuccess, StunClass.ResponseError -> {
                responseReceived(logger, packet, message)
                null
            }
        }
    }

    private fun responseReceived(logger: Logger, packet: ReceivedPacket, message: StunMessage) {
        val request = activeRequests.remove(message.transactionId)
        if (request == null) {
            logger.atTrace()
                .addKeyValue("remoteAddress", packet.source)
                .addKeyValue("tId", message.transactionId)
                .log("Ignoring STUN response because we have no active request for that id.")
            return
        }

        if (request.request.isIntegrityProtected && !message.isIntegrityProtected) {
            logger.atWarn()
                .addKeyValue("remoteAddress", packet.source)
                .addKeyValue("tId", message.transactionId)
                .log("Ignoring STUN response because it was missing HMAC.")
            return
        }

        request.onResponse(UdpStunPacket(packet.timestamp, packet.source, message))
    }

    inner class StunServer(
        val job: Job,
        val password: ByteArray
    ) {
        val knownRemotes = mutableSetOf<InetSocketAddress>()

        val dataReceiveChannel = Channel<ReceivedPacket>(100)
        val stunRequestReceiveChannel = Channel<Pair<ReceivedPacket, StunMessage>>(100)
    }
}
