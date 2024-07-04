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
package gg.essential.sps.quic

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * A single QUIC stream. That is, a bidirectional, reliable and ordered stream of bytes (pretty much like TCP).
 *
 * Closing this stream will (eventually but not necessarily immediately) close the underlying DatagramSocket.
 */
interface QuicStream : Closeable {
    val inputStream: InputStream
    val outputStream: OutputStream
}