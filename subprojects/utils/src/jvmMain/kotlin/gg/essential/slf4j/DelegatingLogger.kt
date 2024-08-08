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
import org.slf4j.event.Level
import org.slf4j.event.LoggingEvent
import org.slf4j.spi.LoggingEventBuilder

/**
 * A logger which delegates to another logger.
 *
 * If the other logger is an instance of our [AbstractEventLogger], then this forwards to its [makeLoggingEventBuilder],
 * allowing that logger to provide a custom instance and/or make extra modifications to the event before it is actually
 * configured.
 * Otherwise it bakes the logging event into the message and forwards via [bakeAndForwardTo].
 */
abstract class DelegatingLogger(val delegate: Logger) : AbstractEventLogger() {
    override fun isTraceEnabled(): Boolean = delegate.isTraceEnabled
    override fun isDebugEnabled(): Boolean = delegate.isDebugEnabled
    override fun isInfoEnabled(): Boolean = delegate.isInfoEnabled
    override fun isWarnEnabled(): Boolean = delegate.isWarnEnabled
    override fun isErrorEnabled(): Boolean = delegate.isErrorEnabled

    override fun log(event: LoggingEvent) {
        forwardTo(delegate, event)
    }

    override fun makeLoggingEventBuilder(level: Level): LoggingEventBuilder {
        // We only delegate this if it's one of our loggers; there's nothing technically wrong with delegating it
        // unconditionally, however Minecraft's log4j configuration doesn't print any of the key-value pairs we add,
        // so those wouldn't be visible if we were to delegate to it.
        // Instead we'll in those cases use the default event builder implementation, which will call our
        // `log(LoggingEvent)` method, which can then bake the pairs into the message before delegating.
        return if (delegate is AbstractEventLogger) {
            delegate.makeLoggingEventBuilder(level)
        } else {
            super.makeLoggingEventBuilder(level)
        }
    }
}
