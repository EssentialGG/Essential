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
package gg.essential.gui.wardrobe.configuration.cosmetic.properties

import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.input.essentialStateTextInput
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledInputRow
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.network.connectionmanager.cosmetics.*
import gg.essential.network.cosmetics.Cosmetic

abstract class SingletonPropertyConfiguration<P : CosmeticProperty>(
    private val clazz: Class<P>,
    private val cosmeticsDataWithChanges: CosmeticsDataWithChanges,
    private val cosmetic: Cosmetic,
) : LayoutDslComponent {

    private val property = cosmetic.properties.firstNotNullOfOrNull { if (clazz.isInstance(it)) clazz.cast(it) else null }

    override fun LayoutScope.layout(modifier: Modifier) {
        box(Modifier.fillWidth().then(modifier)) {
            if (property != null) {
                column(Modifier.fillWidth(), Arrangement.spacedBy(5f)) {
                    layout(property)
                    divider()
                    labeledInputRow("Copy from:") {
                        essentialStateTextInput(
                            mutableStateOf(null),
                            { "" }, // Since we update when we get a valid result, we don't need this
                            { input ->
                                if (input.isBlank())
                                    null
                                else (cosmeticsDataWithChanges.getCosmetic(input)?.properties?.firstNotNullOfOrNull {
                                    if (clazz.isInstance(it)) clazz.cast(it) else null
                                } ?: throw StateTextInput.ParseException())
                            }
                        )
                    }.state.onSetValue(stateScope) {
                        if (it != null) {
                            property.update(it)
                        }
                    }
                }
            } else {
                text("Property was not found?")
            }
        }
    }

    abstract fun LayoutScope.layout(property: P)

    protected fun CosmeticProperty.update(property: P) = cosmeticsDataWithChanges.updateCosmeticProperty(cosmetic.id, this, property)

}
