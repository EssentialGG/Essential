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
package gg.essential.gui.layoutdsl

import gg.essential.elementa.effects.Effect
import gg.essential.gui.elementa.effects.GradientEffect
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.stateOf
import java.awt.Color

fun Modifier.gradient(top: Color, bottom: Color, _desc: GradientVertDesc = GradientDesc) = gradient(stateOf(top), stateOf(bottom), _desc)
fun Modifier.gradient(left: Color, right: Color, _desc: GradientHorzDesc = GradientDesc) = gradient(stateOf(left), stateOf(right), _desc)

fun Modifier.gradient(top: State<Color>, bottom: State<Color>, _desc: GradientVertDesc = GradientDesc) = gradient(top, top, bottom, bottom)
fun Modifier.gradient(left: State<Color>, right: State<Color>, _desc: GradientHorzDesc = GradientDesc) = gradient(left, right, left, right)

sealed interface GradientVertDesc
sealed interface GradientHorzDesc
private object GradientDesc : GradientVertDesc, GradientHorzDesc

fun Modifier.gradient(
    topLeft: State<Color>,
    topRight: State<Color>,
    bottomLeft: State<Color>,
    bottomRight: State<Color>,
) = effect { GradientEffect(topLeft, topRight, bottomLeft, bottomRight) }

private fun Modifier.effect(effect: () -> Effect) = this then {
    val instance = effect()
    enableEffect(instance)
    return@then {
        removeEffect(instance)
    }
}
