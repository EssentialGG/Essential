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
package gg.essential.gui.elementa.state.v2.combinators

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State

infix fun State<Boolean>.and(other: State<Boolean>) =
    zip(other) { a, b -> a && b }

infix fun State<Boolean>.or(other: State<Boolean>) =
    zip(other) { a, b -> a || b }

operator fun State<Boolean>.not() = map { !it }

operator fun MutableState<Boolean>.not() = bimap({ !it }, { !it })
