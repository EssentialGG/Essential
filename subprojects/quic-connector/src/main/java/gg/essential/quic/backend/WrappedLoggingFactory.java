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
package gg.essential.quic.backend;

import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

/**
 * An {@link InternalLoggerFactory} which wraps another factory, redirecting some
 * loggers to a no-op logger.
 */
public class WrappedLoggingFactory extends InternalLoggerFactory {

    private static final HashSet<String> NOOP_LOGGERS;
    static {
        String property = System.getProperty("essential.sps.quic.NOOP_LOGGERS");
        NOOP_LOGGERS = new HashSet<>(property != null ? Arrays.asList(property.split(",")) : Arrays.asList(
            "io.netty.incubator.codec.quic.Quiche" // Overly verbose debug logging
        ));
    }

    private static final MethodHandle mhNewInstance;
    static {
        try {
            Method m = InternalLoggerFactory.class.getDeclaredMethod("newInstance", String.class);
            m.setAccessible(true);
            mhNewInstance = MethodHandles.lookup().unreflect(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final InternalLoggerFactory target;

    public WrappedLoggingFactory(InternalLoggerFactory target) {
        this.target = target;
    }

    @Override
    protected InternalLogger newInstance(String name) {
        if (NOOP_LOGGERS.contains(name)) {
            return new NoOpLogger(name);
        }
        try {
            return (InternalLogger) mhNewInstance.invokeExact(target, name);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static class NoOpLogger implements InternalLogger {
        private final String name;

        public NoOpLogger(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public void trace(String msg) {}

        @Override
        public void trace(String format, Object arg) {}

        @Override
        public void trace(String format, Object argA, Object argB) {}

        @Override
        public void trace(String format, Object... arguments) {}

        @Override
        public void trace(String msg, Throwable t) {}

        @Override
        public void trace(Throwable t) {}

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String msg) {}

        @Override
        public void debug(String format, Object arg) {}

        @Override
        public void debug(String format, Object argA, Object argB) {}

        @Override
        public void debug(String format, Object... arguments) {}

        @Override
        public void debug(String msg, Throwable t) {}

        @Override
        public void debug(Throwable t) {}

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(String msg) {}

        @Override
        public void info(String format, Object arg) {}

        @Override
        public void info(String format, Object argA, Object argB) {}

        @Override
        public void info(String format, Object... arguments) {}

        @Override
        public void info(String msg, Throwable t) {}

        @Override
        public void info(Throwable t) {}

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void warn(String msg) {}

        @Override
        public void warn(String format, Object arg) {}

        @Override
        public void warn(String format, Object... arguments) {}

        @Override
        public void warn(String format, Object argA, Object argB) {}

        @Override
        public void warn(String msg, Throwable t) {}

        @Override
        public void warn(Throwable t) {}

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(String msg) {}

        @Override
        public void error(String format, Object arg) {}

        @Override
        public void error(String format, Object argA, Object argB) {}

        @Override
        public void error(String format, Object... arguments) {}

        @Override
        public void error(String msg, Throwable t) {}

        @Override
        public void error(Throwable t) {}

        @Override
        public boolean isEnabled(InternalLogLevel level) {
            return false;
        }

        @Override
        public void log(InternalLogLevel level, String msg) {}

        @Override
        public void log(InternalLogLevel level, String format, Object arg) {}

        @Override
        public void log(InternalLogLevel level, String format, Object argA, Object argB) {}

        @Override
        public void log(InternalLogLevel level, String format, Object... arguments) {}

        @Override
        public void log(InternalLogLevel level, String msg, Throwable t) {}

        @Override
        public void log(InternalLogLevel level, Throwable t) {}
    }
}
