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
package gg.essential.model.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object AnySerializer : KSerializer<Any> {
    private val inner = JsonElement.serializer()
    override val descriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: Any) =
        encoder.encodeSerializableValue(inner, value.toJsonElement())

    override fun deserialize(decoder: Decoder): Any =
        decoder.decodeSerializableValue(inner).toAny()!!

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toJsonElement(): JsonElement = when (this) {
        is Map<*, *> -> JsonObject((this as Map<String, Any?>).mapValues { it.value.toJsonElement() })
        is List<*> -> JsonArray((this as List<Any?>).map { it.toJsonElement() })
        else -> toJsonPrimitive()
    }

    private fun Any?.toJsonPrimitive(): JsonElement = when (this) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> throw IllegalArgumentException(this.toString())
    }

    private fun JsonElement.toAny(): Any? = when (this) {
        is JsonPrimitive -> toAny()
        is JsonArray -> map { it.toAny() }
        is JsonObject -> mapValues { it.value.toAny() }
    }

    private fun JsonPrimitive.toAny(): Any? = when {
        isString -> content
        content == "null" -> null
        content == "true" -> true
        content == "false" -> false
        else -> content.toIntOrNull() ?: content.toLongOrNull() ?: content.toDoubleOrNull()
    }
}
