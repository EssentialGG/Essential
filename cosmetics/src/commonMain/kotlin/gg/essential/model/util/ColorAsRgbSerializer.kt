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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorAsRgbSerializer : KSerializer<Color> {
    private val inner = Int.serializer()
    override val descriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: Color) =
        encoder.encodeSerializableValue(inner, (value.rgba shr 8).toInt())

    override fun deserialize(decoder: Decoder): Color =
        Color.rgba((decoder.decodeSerializableValue(inner).toUInt() shl 8) or 255u)
}
