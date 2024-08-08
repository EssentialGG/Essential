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
import org.slf4j.event.LoggingEvent
import org.slf4j.spi.LoggingEventAware

/** Forwards logging events to two other loggers. */
class CombinedLogger(
    val a: Logger,
    val b: Logger,
) : AbstractEventLogger(), LoggingEventAware {
    override fun log(event: LoggingEvent) {
        forwardTo(a, event)
        forwardTo(b, event)
    }

    override fun isTraceEnabled(): Boolean = a.isTraceEnabled || b.isTraceEnabled
    override fun isDebugEnabled(): Boolean = a.isDebugEnabled || b.isDebugEnabled
    override fun isInfoEnabled(): Boolean = a.isInfoEnabled || b.isInfoEnabled
    override fun isWarnEnabled(): Boolean = a.isWarnEnabled || b.isWarnEnabled
    override fun isErrorEnabled(): Boolean = a.isErrorEnabled || b.isErrorEnabled
}
