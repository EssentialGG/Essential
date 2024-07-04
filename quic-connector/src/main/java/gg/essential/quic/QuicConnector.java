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
package gg.essential.quic;

import io.netty.channel.nio.NioEventLoopGroup;

import java.io.OutputStream;

public class QuicConnector {

    // To prevent log messages polluting our communication channel, we redirect System.out to System.err
    // This needs to happen ASAP, i.e. before other static initializers.
    // We do first need to preserve the original stdout though, so we can later write data to it.
    protected static final OutputStream stdOut = System.out;
    static {
        System.setOut(System.err);
    }

    // Defaulting to a single thread because that should be enough for the vast majority of use cases (SPS usually does
    // not have 60 people connected).
    private final int THREADS = Integer.parseInt(System.getenv().getOrDefault("ESSENTIAL_QUIC_CONNECTOR_THREADS", "1"));

    protected final NioEventLoopGroup group = new NioEventLoopGroup(THREADS);

    protected QuicConnector() {
    }
}
