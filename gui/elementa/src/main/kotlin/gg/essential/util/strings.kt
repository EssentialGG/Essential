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
package gg.essential.util

import java.awt.Color

/**
 * Colored strings are allowed only in markdown where allowColors is true.
 *
 * @return colored string using color tags (e.g. <color:#ffffff>string</color>)
 */
fun String.colored(color: Color) = "<color:${String.format("#%02X%02X%02X", color.red, color.green, color.blue)}>$this</color>"