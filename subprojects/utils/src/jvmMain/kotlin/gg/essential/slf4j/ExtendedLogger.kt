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
import org.slf4j.spi.LoggingEventBuilder

/** A logger which delegates to another logger while allowing all logging events to be pre-configured. */
class ExtendedLogger(delegate: Logger, val configure: LoggingEventBuilder.() -> Unit) : DelegatingLogger(delegate) {
    override fun makeLoggingEventBuilder(level: Level): LoggingEventBuilder {
        return super.makeLoggingEventBuilder(level).apply(configure)
    }
}

/** Returns a logger which additionally runs [configure] before all logging events. */
fun Logger.with(configure: LoggingEventBuilder.() -> Unit): Logger = ExtendedLogger(this, configure)

/** Returns a logger which adds the given key-value pair to all events logged through it. */
fun Logger.withKeyValue(key: String, value: Any?): Logger = ExtendedLogger(this) { addKeyValue(key, value) }
