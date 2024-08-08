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
package gg.essential.model.light

import dev.folomeev.kotgl.matrix.vectors.Vec3

/** Interface for querying light information at various points in a world. */
interface LightProvider {
    /** Queries the light information at a given world position. */
    fun query(pos: Vec3): Light

    /** Implementation which always returns maximum block and sky light. */
    object FullBright : LightProvider {
        override fun query(pos: Vec3): Light = Light.MAX_VALUE
    }
}
