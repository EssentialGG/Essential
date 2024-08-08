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
package gg.essential.util.lwjgl3.impl.nanovg

import gg.essential.config.AccessedViaReflection
import gg.essential.util.lwjgl3.impl.Bootstrap
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL2
import org.lwjgl.nanovg.NanoVGGL3
import java.awt.Color
import gg.essential.util.lwjgl3.api.nanovg.NanoVG as NanoVGApi

@AccessedViaReflection("NanoVG")
@Suppress("unused") // called via reflection from Lwjgl3Loader
class NanoVGImpl : NanoVGApi {
    private var deleted = false
    var vg: Long = 0
        get() {
            if (field == 0L) {
                if (deleted) {
                    throw IllegalStateException("This NanoVG context has already been deleted!")
                }
                field = if (Bootstrap.gl3.value) {
                    NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS)
                } else {
                    NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS)
                }
                if (field == 0L) {
                    throw RuntimeException("Failed to create nvg context")
                }
            }
            return field
        }

    override fun delete() = (if (Bootstrap.gl3.value) {
        NanoVGGL3.nvgDelete(vg)
    } else {
        NanoVGGL2.nvgDelete(vg)
    }).also {
        deleted = true
        vg = 0
    }

    override fun beginFrame(width: Float, height: Float, devicePixelRatio: Float) = nvgBeginFrame(vg, width, height, devicePixelRatio)

    override fun endFrame() = nvgEndFrame(vg)

    override fun beginPath() = nvgBeginPath(vg)

    override fun rect(x: Float, y: Float, width: Float, height: Float) = nvgRect(vg, x, y, width, height)

    override fun circle(cx: Float, cy: Float, radius: Float) = nvgCircle(vg, cx, cy, radius)

    override fun pathWinding(hole: Boolean) = nvgPathWinding(vg, if (hole) NVG_HOLE else NVG_SOLID)

    override fun fillColor(color: Color) = nvgFillColor(vg, color.toNVG())

    override fun fill() = nvgFill(vg)

    override fun strokeWidth(width: Float) = nvgStrokeWidth(vg, width)

    override fun strokeColor(color: Color) = nvgStrokeColor(vg, color.toNVG())

    override fun startPoint(x: Float, y: Float) = nvgMoveTo(vg, x, y)

    override fun lineTo(x: Float, y: Float) = nvgLineTo(vg, x, y)

    override fun quadBezierTo(cx: Float, cy: Float, x: Float, y: Float) = nvgQuadTo(vg, cx, cy, x, y)

    override fun stroke() = nvgStroke(vg)

    private fun Color.toNVG() = NVGColor.create().also {
        nvgRGBA(red.toByte(), green.toByte(), blue.toByte(), alpha.toByte(), it)
    }
}