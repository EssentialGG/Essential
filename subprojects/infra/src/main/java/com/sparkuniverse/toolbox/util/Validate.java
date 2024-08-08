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
package com.sparkuniverse.toolbox.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class Validate {

    private static final Supplier<String>
            DEFAULT_NULL_MESSAGE = () -> "Object cannot be null.",
            DEFAULT_IS_TRUE_MESSAGE = () -> "Object cannot be false.";

    public static <T> T notNull(@Nullable final T object) {
        return notNull(object, DEFAULT_NULL_MESSAGE);
    }

    public static <T> T notNull(@Nullable final T object, @NotNull final Supplier<String> message, @Nullable final Object... values) {
        if (object == null) {
            throw new NullPointerException(String.format(message.get(), values));
        }

        return object;
    }

    public static void isTrue(final boolean expression) {
        isTrue(expression, DEFAULT_IS_TRUE_MESSAGE);
    }

    public static void isTrue(final boolean expression, @NotNull final Supplier<String> message, final Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message.get(), values));
        }
    }

}
