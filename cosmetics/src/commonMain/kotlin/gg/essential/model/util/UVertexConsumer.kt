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

import gg.essential.model.light.Light

interface UVertexConsumer {
    fun pos(stack: UMatrixStack, x: Double, y: Double, z: Double): UVertexConsumer

    fun tex(u: Double, v: Double): UVertexConsumer

    fun norm(stack: UMatrixStack, x: Float, y: Float, z: Float): UVertexConsumer

    fun color(color: Color): UVertexConsumer

    fun light(light: Light): UVertexConsumer

    fun endVertex(): UVertexConsumer
}
