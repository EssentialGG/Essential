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

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.event.LoggingEvent
import org.slf4j.helpers.AbstractLogger
import org.slf4j.spi.LoggingEventAware

/**
 * Abstract [Logger] implementation which forwards all logging calls to a single [LoggingEvent]-based [log] method.
 *
 * Note: Supports neither marker-aware levels nor caller-aware logging.
 * Since we don't control the log4j configs, we can't really make use of those anyway.
 */
abstract class AbstractEventLogger : AbstractLogger(), LoggingEventAware {
    override fun isTraceEnabled(marker: Marker?): Boolean = isTraceEnabled
    override fun isDebugEnabled(marker: Marker?): Boolean = isDebugEnabled
    override fun isInfoEnabled(marker: Marker?): Boolean = isInfoEnabled
    override fun isWarnEnabled(marker: Marker?): Boolean = isWarnEnabled
    override fun isErrorEnabled(marker: Marker?): Boolean = isErrorEnabled
    override fun getFullyQualifiedCallerName(): String = javaClass.name

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        msg: String,
        arguments: Array<out Any?>?,
        throwable: Throwable?
    ) {
        atLevel(level).apply {
            addMarker(marker)
            setMessage(msg)
            arguments?.forEach { addArgument(it) }
            setCause(throwable)
        }.log()
    }

    abstract override fun log(event: LoggingEvent)

    /**
     * Forwards the given logging event to the given logger.
     *
     * Any key-value pairs are baked into the message. This is because MC's log4j configuration does not support these
     * so they'd be silently ignored if we did not bake them.
     */
    protected fun bakeAndForwardTo(delegate: Logger, event: LoggingEvent) {
        val keyValuePairs = event.keyValuePairs
        val message =
            if (keyValuePairs != null && keyValuePairs.isNotEmpty()) {
                keyValuePairs.joinToString(" ", "", " ") { "${it.key}=${it.value}" } + event.message
            } else {
                event.message
            }

        delegate.atLevel(event.level).apply {
            setMessage(message)
            event.arguments?.forEach { addArgument(it) }
            setCause(event.throwable)
        }.log()
    }

    /**
     * Forwards the given logging event to the given logger.
     */
    protected fun forwardTo(delegate: AbstractEventLogger, event: LoggingEvent) {
        delegate.atLevel(event.level).apply {
            event.keyValuePairs?.forEach { addKeyValue(it.key, it.value) }
            setMessage(event.message)
            event.arguments?.forEach { addArgument(it) }
            setCause(event.throwable)
        }.log()
    }

    /**
     * Forwards the given logging event to the given logger.
     *
     * Note: If the given logger is not a [AbstractEventLogger], any key-value pairs are baked into the message to
     * prevent them from being silently lost. See [bakeAndForwardTo].
     */
    protected fun forwardTo(delegate: Logger, event: LoggingEvent) {
        if (delegate is AbstractEventLogger) {
            forwardTo(delegate, event)
        } else {
            bakeAndForwardTo(delegate, event)
        }
    }
}