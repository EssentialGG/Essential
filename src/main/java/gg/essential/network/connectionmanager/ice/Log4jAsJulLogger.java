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
package gg.essential.network.connectionmanager.ice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log4jAsJulLogger extends Logger {
    private static final Map<Level, org.apache.logging.log4j.Level> levelMap = new ConcurrentHashMap<>();
    static {
        levelMap.put(Level.ALL, org.apache.logging.log4j.Level.ALL);
        levelMap.put(Level.FINEST, org.apache.logging.log4j.Level.TRACE);
        levelMap.put(Level.FINER, org.apache.logging.log4j.Level.TRACE);
        levelMap.put(Level.FINE, org.apache.logging.log4j.Level.DEBUG);
        levelMap.put(Level.CONFIG, org.apache.logging.log4j.Level.DEBUG);
        levelMap.put(Level.INFO, org.apache.logging.log4j.Level.INFO);
        levelMap.put(Level.WARNING, org.apache.logging.log4j.Level.WARN);
        levelMap.put(Level.SEVERE, org.apache.logging.log4j.Level.ERROR);
        levelMap.put(Level.OFF, org.apache.logging.log4j.Level.OFF);
    }

    private final org.apache.logging.log4j.Logger logger;

    public Log4jAsJulLogger(final String name) {
        this(LogManager.getLogger(name));
    }

    public Log4jAsJulLogger(final org.apache.logging.log4j.Logger logger) {
        super(logger.getName(), null);
        super.setLevel(Level.FINEST);
        this.logger = logger;
    }

    private static org.apache.logging.log4j.Level mapLevel(Level julLevel) {
        return levelMap.getOrDefault(julLevel, org.apache.logging.log4j.Level.INFO);
    }

    @Override
    public void log(final LogRecord record) {
        final Filter filter = getFilter();
        if (filter != null && !filter.isLoggable(record)) {
            return;
        }
        final org.apache.logging.log4j.Level level = mapLevel(record.getLevel());
        final Message message = logger.getMessageFactory().newMessage(record.getMessage(), record.getParameters());
        final Throwable thrown = record.getThrown();
        logger.log(level, (Marker) null, message, thrown);
    }

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isEnabled(mapLevel(level));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public void setLevel(final Level newLevel) throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParent(final Logger parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void log(final Level level, final String msg) {
        logger.log(mapLevel(level), msg);
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        logger.log(mapLevel(level), msg, param1);
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        logger.log(mapLevel(level), msg, params);
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        logger.log(mapLevel(level), msg, thrown);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        logger.entry();
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        logger.entry(param1);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        logger.entry(params);
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.exit();
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        logger.exit(result);
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logger.throwing(thrown);
    }
}
