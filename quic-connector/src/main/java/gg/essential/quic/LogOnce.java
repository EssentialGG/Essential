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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LogOnce {
    private final Consumer<String> consumer;
    private final Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private LogOnce(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    public void log(String key) {
        if (seen.add(key)) {
            consumer.accept(key);
        }
    }

    public void log(String key, Object message) {
        if (seen.add(key)) {
            consumer.accept(key + ": " + message);
        }
    }

    public void log(String key, Supplier<Object> message) {
        if (seen.add(key)) {
            consumer.accept(key + ": " + message.get());
        }
    }

    public static LogOnce toForkedJvmDebug() {
        return new LogOnce(message -> System.err.println("[DEBUG] " + message));
    }

    public static LogOnce to(Consumer<String> consumer) {
        return new LogOnce(consumer);
    }
}
