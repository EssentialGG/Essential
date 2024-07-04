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
package org.slf4j.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Log4jLoggerFactory implements ILoggerFactory {
    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, __ -> {
            String log4jName = name.equals(Logger.ROOT_LOGGER_NAME) ? LogManager.ROOT_LOGGER_NAME : name;
            org.apache.logging.log4j.Logger log4jLogger = LogManager.getLogger(log4jName);
            if (log4jLogger instanceof AbstractLogger) {
                return new Log4jLogger((AbstractLogger) log4jLogger, name);
            } else {
                throw new UnsupportedOperationException("slf4j-to-log4j bridge requires logger extending AbstractLogger but got " + log4jLogger.getClass());
            }
        });
    }
}