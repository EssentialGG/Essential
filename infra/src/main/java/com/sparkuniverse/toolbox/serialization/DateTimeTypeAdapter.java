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
package com.sparkuniverse.toolbox.serialization;

import gg.essential.lib.gson.TypeAdapter;
import gg.essential.lib.gson.stream.JsonReader;
import gg.essential.lib.gson.stream.JsonToken;
import gg.essential.lib.gson.stream.JsonWriter;
import com.sparkuniverse.toolbox.util.DateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class DateTimeTypeAdapter extends TypeAdapter<DateTime> {

    @Override
    public void write(@NotNull final JsonWriter writer, @Nullable final DateTime value) throws IOException {
        if (value == null) {
            writer.nullValue();
        } else writer.value(value.getTime());
    }

    @Override
    public DateTime read(@NotNull final JsonReader reader) throws IOException {
        if (JsonToken.NULL == reader.peek()) {
            reader.nextNull();;
            return null;
        }

        return new DateTime(reader.nextLong());
    }

}
