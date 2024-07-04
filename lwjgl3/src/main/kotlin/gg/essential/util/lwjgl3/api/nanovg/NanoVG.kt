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
package gg.essential.util.lwjgl3.api.nanovg

import java.awt.Color

interface NanoVG {
    fun delete()

    fun beginFrame(width: Float, height: Float, devicePixelRatio: Float)
    fun endFrame()

    fun beginPath()
    fun rect(x: Float, y: Float, width: Float, height: Float)
    fun circle(cx: Float, cy: Float, radius: Float)
    fun pathWinding(hole: Boolean)
    fun fillColor(color: Color)
    fun fill()
    fun strokeWidth(width: Float)
    fun strokeColor(color: Color)
    fun startPoint(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun quadBezierTo(cx: Float, cy: Float, x: Float, y: Float)
    fun stroke()
}