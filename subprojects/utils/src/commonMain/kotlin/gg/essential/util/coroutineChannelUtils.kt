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
package gg.essential.util

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select

suspend fun <T> forwardChannelsInto(merged: SendChannel<T>, vararg channels: ReceiveChannel<T>) {
    val activeChannels = channels.toMutableList()
    while (activeChannels.isNotEmpty()) {
        val (channel, result) = select {
            for (channel in activeChannels) {
                channel.onReceiveCatching { result -> Pair(channel, result) }
            }
        }
        if (result.isFailure) {
            if (result.isClosed) {
                activeChannels.remove(channel)
            } else {
                merged.close(result.exceptionOrNull())
                return
            }
        } else {
            merged.send(result.getOrThrow())
        }
    }
    merged.close()
}
