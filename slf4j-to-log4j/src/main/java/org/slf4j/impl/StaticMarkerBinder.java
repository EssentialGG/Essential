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
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MarkerFactoryBinder;

@AccessedViaReflection("Bootstrap.initialize") // actual caller is slf4j because of the specific package+name this has
@SuppressWarnings("unused") // referenced by slf4j-api
public class StaticMarkerBinder implements MarkerFactoryBinder {
    // (public cause referenced from slf4j-api)
    public static final StaticMarkerBinder SINGLETON = new StaticMarkerBinder();

    private final BasicMarkerFactory markerFactory = new BasicMarkerFactory();

    private StaticMarkerBinder() {
    }

    @AccessedViaReflection("StaticMarkerBinder")
    @SuppressWarnings("unused") // called by slf4j-api
    public static StaticMarkerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public String getMarkerFactoryClassStr() {
        return BasicMarkerFactory.class.getName();
    }
}
