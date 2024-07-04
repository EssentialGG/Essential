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
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TreeMapSerializer::class)
class TreeMap<K : Comparable<K>, V> private constructor(
    private val list: List<Map.Entry<K, V>>,
    private val map: Map<K, V> = list.associate { (k, v) -> k to v },
) : Map<K, V> by map {

    constructor(map: Map<K, V>) : this(map.entries.sortedBy { it.key })

    fun lowerEntry(key: K): Map.Entry<K, V>? =
        findEntry(below = true, inclusive = false, key)

    fun floorEntry(key: K): Map.Entry<K, V>? =
        findEntry(below = true, inclusive = true, key)

    fun ceilingEntry(key: K): Map.Entry<K, V>? =
        findEntry(below = false, inclusive = true, key)

    fun higherEntry(key: K): Map.Entry<K, V>? =
        findEntry(below = false, inclusive = false, key)

    private fun findEntry(below: Boolean, inclusive: Boolean, key: K): Map.Entry<K, V>? {
        if (list.isEmpty()) return null
        val index = list.binarySearchBy(key) { it.key }
        return list.getOrNull(
            if (index >= 0) {
                when {
                    inclusive -> index
                    below -> index - 1
                    else -> index + 1
                }
            } else {
                val insertionPoint = -index - 1
                when {
                    below -> insertionPoint - 1
                    else -> insertionPoint
                }
            }
        )
    }

    fun lowestEntry(): Map.Entry<K, V>? = list.firstOrNull()

    fun lastKey(): K? = list.lastOrNull()?.key

    override fun toString(): String = map.toString()
    override fun hashCode(): Int = map.hashCode()
    override fun equals(other: Any?): Boolean = (other as? TreeMap<*, *>)?.list?.equals(list) == true
}

private class TreeMapSerializer<K : Comparable<K>, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) : KSerializer<TreeMap<K, V>> {
    private val inner = MapSerializer(kSerializer, vSerializer)
    override val descriptor = inner.descriptor
    override fun serialize(encoder: Encoder, value: TreeMap<K, V>) = encoder.encodeSerializableValue(inner, value)
    override fun deserialize(decoder: Decoder): TreeMap<K, V> = TreeMap(decoder.decodeSerializableValue(inner))
}
