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
package gg.essential.config

import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.vigilancev2.VigilanceV2SettingsGui
import gg.essential.vigilance.data.PropertyData

class GuiFactory internal constructor(
    private val properties: ListState<PropertyData>,
) {
    operator fun invoke(category: String? = null): VigilanceV2SettingsGui {
        return VigilanceV2SettingsGui(properties, category)
    }
}
