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
package gg.essential.model.file

import gg.essential.cosmetics.events.AnimationEvent
import gg.essential.model.Channels
import gg.essential.model.Keyframe
import gg.essential.model.Keyframes
import gg.essential.model.molang.MolangExpression
import gg.essential.model.molang.MolangVec3
import gg.essential.model.molang.parseMolangExpression
import gg.essential.model.util.ListOrSingle
import gg.essential.model.util.TreeMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@Serializable
data class AnimationFile(
    @SerialName("format_version")
    val formatVersion: String,
    val animations: Map<String, Animation> = emptyMap(),
    val triggers: List<AnimationEvent> = emptyList(),
) {
    @Serializable
    data class Animation(
        val loop: Loop = Loop.False,
        @SerialName("animation_length")
        val animationLength: Float? = null,
        val bones: Map<String, Channels> = emptyMap(),
        @SerialName("particle_effects")
        val particleEffects: Map<Float, ListOrSingle<ParticleEffect>> = emptyMap(),
        @SerialName("sound_effects")
        val soundEffects: Map<Float, ListOrSingle<SoundEffect>> = emptyMap(),
    ) {
        @Serializable
        data class ParticleEffect(
            val effect: String,
            val locator: String? = null,
            @SerialName("pre_effect_script")
            val preEffectScript: MolangExpression? = null,
        )

        @Serializable
        data class SoundEffect(
            val effect: String,
            val locator: String? = null,
        )
    }

    @Serializable(with = LoopSerializer::class)
    enum class Loop {
        False,
        True,
        HoldOnLastFrame,
    }
}

internal class LoopSerializer : KSerializer<AnimationFile.Loop> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): AnimationFile.Loop =
        when (val json = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content) {
            "false" -> AnimationFile.Loop.False
            "true" -> AnimationFile.Loop.True
            "hold_on_last_frame" -> AnimationFile.Loop.HoldOnLastFrame
            else -> throw IllegalArgumentException("Unexpected value \"$json\"")
        }

    override fun serialize(encoder: Encoder, value: AnimationFile.Loop) =
        (encoder as JsonEncoder).encodeJsonElement(
            when (value) {
                AnimationFile.Loop.False -> JsonPrimitive(false)
                AnimationFile.Loop.True -> JsonPrimitive(true)
                AnimationFile.Loop.HoldOnLastFrame -> JsonPrimitive("hold_on_last_frame")
            }
        )
}

internal class KeyframesSerializer : KSerializer<Keyframes> {
    override val descriptor: SerialDescriptor = InnerSerializer.descriptor
    override fun deserialize(decoder: Decoder): Keyframes = Keyframes(InnerSerializer.deserialize(decoder))
    override fun serialize(encoder: Encoder, value: Keyframes) = InnerSerializer.serialize(encoder, value.frames)

    private object InnerSerializer : JsonTransformingSerializer<TreeMap<Float, Keyframe>>(serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            if (element is JsonObject) element else buildJsonObject { put("0", element) }
    }
}

internal object KeyframeSerializer : KSerializer<Keyframe> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor
    override fun deserialize(decoder: Decoder): Keyframe = parse((decoder as JsonDecoder).decodeJsonElement())
    override fun serialize(encoder: Encoder, value: Keyframe) = throw UnsupportedOperationException()

    private fun parse(json: JsonElement): Keyframe = with(json) {
        fun JsonElement.parseMolangVector(): MolangVec3 = if (this is JsonArray) {
            if (size == 3) {
                MolangVec3(
                    (get(0) as JsonPrimitive).parseMolangExpression(),
                    (get(1) as JsonPrimitive).parseMolangExpression(),
                    (get(2) as JsonPrimitive).parseMolangExpression()
                )
            } else {
                (get(0) as JsonPrimitive).parseMolangExpression().let { MolangVec3(it, it, it) }
            }
        } else {
            (this as JsonPrimitive).parseMolangExpression().let { MolangVec3(it, it, it) }
        }
        if (this is JsonObject) {
            val pre = get("pre")?.parseMolangVector()
            val post = get("post")!!.parseMolangVector()
            val smooth = get("lerp_mode")?.jsonPrimitive?.contentOrNull == "catmullrom"
            Keyframe(pre ?: post, post, smooth)
        } else {
            parseMolangVector().let { Keyframe(it, it, false) }
        }
    }
}
