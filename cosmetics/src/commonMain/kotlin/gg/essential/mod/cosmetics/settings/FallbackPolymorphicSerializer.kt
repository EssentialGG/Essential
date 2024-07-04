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
package gg.essential.mod.cosmetics.settings

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

/** Serializer which uses a special Unknown subclass as a fallback when the polymorphic serializer does not have an exact match. */
// While we could use the generated sealed class serializer directly with `polymorphicDefaultDeserializer` to
// decode Unknown properties, it'll complain during encoding that Unknown has an overlapping `type` field that
// conflicts with the discriminator field.
// We cannot use a wrapper to decide between the sealed class serializer and the Unknown serializer because
// `@Serializer` is broken: https://github.com/Kotlin/kotlinx.serialization/issues/970
// So we'll just use a different intermediate name for the Unknown `type` and transform the json afterwards:
@OptIn(ExperimentalSerializationApi::class)
abstract class FallbackPolymorphicSerializer<T : Any>(
    private val baseClass: KClass<T>,
    private val discriminatorField: String,
    private val unknownDiscriminatorField: String,
    private val unknownSerialName: String,
) : JsonTransformingSerializer<T>(PolymorphicSerializer(baseClass)) {
    // And because we can't use the auto-generated polymorphic serializer at the same time, we need to create one
    // manually:
    abstract val module: SerializersModule

    override fun transformSerialize(element: JsonElement): JsonElement {
        return if (unknownDiscriminatorField in element.jsonObject) {
            JsonObject(element.jsonObject.toMutableMap().apply {
                put(discriminatorField, remove(unknownDiscriminatorField)!!)
            })
        } else element
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val type = element.jsonObject.getValue(discriminatorField).jsonPrimitive.content
        return if (module.getPolymorphic(baseClass, type.uppercase()) != null) {
            // FIXME: Temporary workaround for the fact that some types are not capitalized in assets/database
            JsonObject(element.jsonObject + (discriminatorField to JsonPrimitive(type.uppercase())))
        } else if (module.getPolymorphic(baseClass, type) == null) {
            JsonObject(element.jsonObject.toMutableMap().apply {
                put(unknownDiscriminatorField, JsonPrimitive(type))
                put(discriminatorField, JsonPrimitive(unknownSerialName))
            })
        } else element
    }
}
