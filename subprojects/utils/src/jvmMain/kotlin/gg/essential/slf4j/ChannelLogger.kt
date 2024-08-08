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
package gg.essential.slf4j

import kotlinx.coroutines.channels.Channel
import org.slf4j.event.Level
import org.slf4j.event.LoggingEvent
import org.slf4j.spi.LoggingEventAware
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * A logger implementation which forwards logging events into a Kotlin Coroutines [Channel].
 *
 * Because coroutine channels are not necessarily synchronous, it also includes a timestamp from the given time source
 * with each event.
 */
class ChannelLogger(
    private val level: Level,
    private val channel: Channel<Pair<ComparableTimeMark, LoggingEvent>>,
    private val timeSource: TimeSource.WithComparableMarks,
) : AbstractEventLogger(), LoggingEventAware {
    override fun log(event: LoggingEvent) {
        channel.trySend(Pair(timeSource.markNow(), event))
    }

    override fun isTraceEnabled(): Boolean = level >= Level.TRACE
    override fun isDebugEnabled(): Boolean = level >= Level.DEBUG
    override fun isInfoEnabled(): Boolean = level >= Level.INFO
    override fun isWarnEnabled(): Boolean = level >= Level.WARN
    override fun isErrorEnabled(): Boolean = true // don't use it if you don't want it
}
