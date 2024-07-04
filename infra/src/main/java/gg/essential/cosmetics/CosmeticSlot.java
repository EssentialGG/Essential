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
package gg.essential.cosmetics;

import gg.essential.lib.gson.JsonDeserializationContext;
import gg.essential.lib.gson.JsonDeserializer;
import gg.essential.lib.gson.JsonElement;
import gg.essential.lib.gson.JsonParseException;
import gg.essential.lib.gson.JsonPrimitive;
import gg.essential.lib.gson.JsonSerializationContext;
import gg.essential.lib.gson.JsonSerializer;
import gg.essential.lib.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

@JsonAdapter(CosmeticSlot.Serializer.class)
public class CosmeticSlot {
    private static final ConcurrentHashMap<String, CosmeticSlot> ENTRIES = new ConcurrentHashMap<>();

    public final String id;

    private CosmeticSlot(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CosmeticSlot)) return false;
        CosmeticSlot that = (CosmeticSlot) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return this.id;
    }

    public static CosmeticSlot of(String id) {
        return ENTRIES.computeIfAbsent(id, CosmeticSlot::new);
    }

    public static class Serializer implements JsonSerializer<CosmeticSlot>, JsonDeserializer<CosmeticSlot> {
        @Override
        public CosmeticSlot deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return of(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(CosmeticSlot cosmeticSlot, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(cosmeticSlot.id);
        }
    }
}
