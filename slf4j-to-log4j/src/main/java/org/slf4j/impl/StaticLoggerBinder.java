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

import gg.essential.config.AccessedViaReflection;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

@AccessedViaReflection("Bootstrap.initialize") // actual caller is slf4j because of the specific package+name this has
public class StaticLoggerBinder implements LoggerFactoryBinder {
    @AccessedViaReflection("StaticLoggerBinder")
    @SuppressWarnings("unused") // read by slf4j-api
    public static final String REQUESTED_API_VERSION = "1.7.36";

    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private final ILoggerFactory loggerFactory = new Log4jLoggerFactory();

    private StaticLoggerBinder() {
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return Log4jLoggerFactory.class.getName();
    }

    @AccessedViaReflection("StaticLoggerBinder")
    @SuppressWarnings("unused") // called by slf4j-api
    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }
}
