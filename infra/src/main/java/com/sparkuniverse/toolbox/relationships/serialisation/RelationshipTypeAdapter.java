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
package com.sparkuniverse.toolbox.relationships.serialisation;

import gg.essential.lib.gson.TypeAdapter;
import gg.essential.lib.gson.stream.JsonReader;
import gg.essential.lib.gson.stream.JsonWriter;
import com.sparkuniverse.toolbox.relationships.enums.RelationshipType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class RelationshipTypeAdapter extends TypeAdapter<RelationshipType> {

    @Override
    public void write(@NotNull final JsonWriter writer, @Nullable final RelationshipType value) throws IOException {
        if (value == null) {
            writer.nullValue();
        } else writer.value(value.toString());
    }

    @Override
    public @Nullable RelationshipType read(@NotNull final JsonReader reader) throws IOException {
        final String value = reader.nextString();

        if (value == null) {
            return null;
        }

        try {
            return RelationshipType.valueOf(value);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

}
