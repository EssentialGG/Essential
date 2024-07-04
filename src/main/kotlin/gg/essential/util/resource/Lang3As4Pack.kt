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
package gg.essential.util.resource

import gg.essential.Essential.GSON
import net.minecraft.client.resources.IResourcePack
import java.io.InputStream

class Lang3As4Pack(parent: IResourcePack) : ResourcePackAdapter(parent) {
    override fun mapToParent(path: String): FileMapper? {
        if (path.startsWith("lang/") && path.endsWith(".json")) {
            return JsonFromProp(path)
        }
        return null
    }

    class JsonFromProp(path: String) : FileMapper {
        override val parentPath: String = path.substring(0, path.length - 4) + "lang"

        override fun map(stream: InputStream): InputStream {
            val entries = mutableMapOf<String, String>()
            stream.use {
                stream.bufferedReader().forEachLine {
                    val line = it.trim()
                    if (line.isEmpty() || line.startsWith("#")) {
                        return@forEachLine
                    }
                    val (key, value) = line.split('=', limit = 2)
                    entries[key] = value
                }
            }
            return GSON.toJson(entries).byteInputStream()
        }
    }
}
