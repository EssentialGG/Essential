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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.spi.LocationAwareLogger;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class Log4jLogger extends MarkerIgnoringBase implements LocationAwareLogger {

    private static final String FQCN = Log4jLogger.class.getName();

    private final AbstractLogger log4j;

    public Log4jLogger(AbstractLogger log4j, String name) {
        this.log4j = log4j;
        this.name = name;
    }

    @Override
    public boolean isTraceEnabled() {
        return log4j.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            log(Level.TRACE, new SimpleMessage(msg), null);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            log(Level.TRACE, new FormattedMessage(format, arg), null);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            log(Level.TRACE, new FormattedMessage(format, arg1, arg2), null);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            log(Level.TRACE, new FormattedMessage(format, arguments), null);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            log(Level.TRACE, new SimpleMessage(msg), t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return log4j.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, new SimpleMessage(msg), null);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, new FormattedMessage(format, arg), null);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, new FormattedMessage(format, arg1, arg2), null);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, new FormattedMessage(format, arguments), null);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, new SimpleMessage(msg), t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return log4j.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            log(Level.INFO, new SimpleMessage(msg), null);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            log(Level.INFO, new FormattedMessage(format, arg), null);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            log(Level.INFO, new FormattedMessage(format, arg1, arg2), null);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            log(Level.INFO, new FormattedMessage(format, arguments), null);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            log(Level.INFO, new SimpleMessage(msg), t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return log4j.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            log(Level.WARN, new SimpleMessage(msg), null);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            log(Level.WARN, new FormattedMessage(format, arg), null);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            log(Level.WARN, new FormattedMessage(format, arg1, arg2), null);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            log(Level.WARN, new FormattedMessage(format, arguments), null);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            log(Level.WARN, new SimpleMessage(msg), t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return log4j.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            log(Level.ERROR, new SimpleMessage(msg), null);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            log(Level.ERROR, new FormattedMessage(format, arg), null);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            log(Level.ERROR, new FormattedMessage(format, arg1, arg2), null);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            log(Level.ERROR, new FormattedMessage(format, arguments), null);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            log(Level.ERROR, new SimpleMessage(msg), t);
        }
    }

    @Override
    public void log(org.slf4j.Marker marker, String fqcn, int levelInt, String message, Object[] argArray, Throwable t) {
        Level level;
        switch (levelInt) {
            case TRACE_INT: level = Level.TRACE; break;
            case DEBUG_INT: level = Level.DEBUG; break;
            case INFO_INT: level = Level.INFO; break;
            case WARN_INT: level = Level.WARN; break;
            case ERROR_INT:
            default: level = Level.ERROR; break;
        }
        if (log4j.isEnabled(level)) {
            Message data = argArray == null ? new SimpleMessage(message) : new ParameterizedMessage(message, argArray, t);
            AbstractLoggerAccessor.INSTANCE.log(log4j, fqcn, level, null, data, t);
        }
    }

    private void log(Level level, Message data, Throwable t) {
        AbstractLoggerAccessor.INSTANCE.log(log4j, FQCN, level, null, data, t);
    }

    private interface AbstractLoggerAccessor {
        void log(AbstractLogger logger, String fqcn, Level level, Marker marker, Message message, Throwable t);

        AbstractLoggerAccessor INSTANCE = get();

        static AbstractLoggerAccessor get() {
            try {
                Class<?>[] args = new Class[] { String.class, Level.class, Marker.class, Message.class, Throwable.class };
                @SuppressWarnings("JavaReflectionMemberAccess")
                Method method = AbstractLogger.class.getMethod("logMessage", args);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                return (AbstractLoggerAccessor) LambdaMetafactory.metafactory(
                    lookup,
                    "log",
                    MethodType.methodType(AbstractLoggerAccessor.class),
                    MethodType.methodType(void.class, AbstractLogger.class, args),
                    lookup.unreflect(method),
                    MethodType.methodType(void.class, AbstractLogger.class, args)
                ).getTarget().invokeExact();
            } catch (NoSuchMethodException e) {
                return BetaAbstractLoggerAccessor::log;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static abstract class BetaAbstractLoggerAccessor extends AbstractLogger {
        static void log(AbstractLogger logger, String fqcn, Level level, Marker marker, Message data, Throwable t) {
            logger.log(marker, fqcn, level, data, t);
        }
    }
}
