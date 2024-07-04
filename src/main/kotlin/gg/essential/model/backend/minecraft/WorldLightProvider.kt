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
package gg.essential.model.backend.minecraft

import dev.folomeev.kotgl.matrix.vectors.Vec3
import dev.folomeev.kotgl.matrix.vectors.mutables.floor
import gg.essential.model.light.Light
import gg.essential.model.light.LightProvider
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.util.math.BlockPos

//#if MC>=11600
//$$ import net.minecraft.client.renderer.WorldRenderer
//#endif

class WorldLightProvider(private val world: WorldClient) : LightProvider {
    override fun query(pos: Vec3): Light {
        val block = with(pos.floor()) { BlockPos(x.toInt(), y.toInt(), z.toInt()) }
        if (!world.isBlockLoaded(block)) {
            return Light.MIN_VALUE
        }
        //#if MC>=11600
        //$$ return Light(WorldRenderer.getCombinedLight(world, block).toUInt())
        //#else
        return Light(world.getCombinedLight(block, 0).toUInt())
        //#endif
    }
}
