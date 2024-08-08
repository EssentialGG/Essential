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
package gg.essential.slf4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.spi.LoggingEventAware;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class Log4jLogger extends LegacyAbstractLogger /* "Legacy" because we don't support markers */ implements LoggingEventAware {

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
    public boolean isDebugEnabled() {
        return log4j.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return log4j.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return log4j.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return log4j.isErrorEnabled();
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return FQCN;
    }

    @Override
    protected void handleNormalizedLoggingCall(
        org.slf4j.event.Level level,
        org.slf4j.Marker marker,
        String msg,
        Object[] arguments,
        Throwable throwable
    ) {
        String fqcn = getFullyQualifiedCallerName();
        Message data = arguments == null ? new SimpleMessage(msg) : new ParameterizedMessage(msg, arguments, throwable);
        AbstractLoggerAccessor.INSTANCE.log(log4j, fqcn, convertLevel(level), null, data, throwable);
    }

    @Override
    public void log(LoggingEvent event) {
        String fqcn = event.getCallerBoundary();
        if (fqcn == null) fqcn = FQCN;
        String msg = event.getMessage();
        Object[] arguments = event.getArgumentArray();
        Throwable throwable = event.getThrowable();
        Message data = arguments == null ? new SimpleMessage(msg) : new ParameterizedMessage(msg, arguments, throwable);
        AbstractLoggerAccessor.INSTANCE.log(log4j, fqcn, convertLevel(event.getLevel()), null, data, throwable);
    }

    private Level convertLevel(org.slf4j.event.Level level) {
        switch (level) {
            case TRACE: return Level.TRACE;
            case DEBUG: return Level.DEBUG;
            case INFO: return Level.INFO;
            case WARN: return Level.WARN;
            case ERROR:
            default: return Level.ERROR;
        }
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
