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
package gg.essential.gui.wardrobe.configuration

import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.FeaturedPageWidth
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialDropDown
import gg.essential.gui.common.compactFullEssentialToggle
import gg.essential.gui.common.input.StateTextInput
import gg.essential.gui.common.input.essentialStateTextInput
import gg.essential.gui.common.modal.CancelableInputModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledISODateInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledListInputRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.labeledRow
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.navButton
import gg.essential.gui.wardrobe.configuration.cosmetic.settings.*
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.featured.FeaturedItem
import gg.essential.mod.cosmetics.featured.FeaturedPage
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.mod.cosmetics.settings.CosmeticSettingType
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.time.Duration
import java.time.Instant

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class FeaturedPageCollectionConfiguration(
    state: WardrobeState,
) : AbstractConfiguration<FeaturedPageCollectionId, FeaturedPageCollection>(
    ConfigurationType.FEATURED_PAGE_LAYOUT_COLLECTIONS,
    state
) {

    override fun LayoutScope.columnLayout(pageCollection: FeaturedPageCollection) {
        navButton("Add new page") {
            GuiUtil.pushModal { manager ->
                CancelableInputModal(manager, "Featured page width").configure {
                    titleText = "Create New Featured Page"
                    contentText = "Enter the width for the new Featured Page."
                }.apply {
                    onPrimaryActionWithValue { id ->
                        val pageId: FeaturedPageWidth = try {
                            id.toInt()
                        } catch (e: Exception) {
                            setError("Not an integer!")
                            return@onPrimaryActionWithValue
                        }
                        if (pageCollection.pages.containsKey(pageId)) {
                            setError("That width already exists!")
                            return@onPrimaryActionWithValue
                        }
                        pageCollection.update(pageCollection.copy(pages = pageCollection.pages + (pageId to FeaturedPage(listOf()))))
                    }
                }
            }
        }
        spacer(height = 5f)
        val availability = pageCollection.availability
        val availabilityState = mutableStateOf(availability != null)
        availabilityState.onSetValue(stateScope) {
            if (it) {
                pageCollection.update(pageCollection.copy(availability = FeaturedPageCollection.Availability(Instant.now(), Instant.now().plus(Duration.ofDays(1)))))
            } else {
                pageCollection.update(pageCollection.copy(availability = null))
            }
        }
        labeledRow("Availability: ") {
            box(Modifier.childBasedWidth(3f).childBasedHeight(3f).hoverScope()) {
                compactFullEssentialToggle(availabilityState.toV1(stateScope))
                spacer(1f, 1f)
            }
        }
        if (availability != null) {
            labeledISODateInputRow("After:", mutableStateOf(availability.after)).state.onSetValue(stateScope) {
                pageCollection.update(pageCollection.copy(availability = availability.copy(after = it)))
            }
            labeledISODateInputRow("Until:", mutableStateOf(availability.until)).state.onSetValue(stateScope) {
                pageCollection.update(pageCollection.copy(availability = availability.copy(until = it)))
            }
        }
        spacer(height = 5f)
        submenuSelection(pageCollection)
    }

    override fun getSubmenus(editing: FeaturedPageCollection): Set<AbstractConfigurationSubmenu<FeaturedPageCollection>> {
        return editing.pages.entries.map { FeaturedPageConfigurationSubmenu(it.key.toString(), "${it.key}-wide page", editing, it.key, it.value) }.toSet()
    }

    private inner class FeaturedPageConfigurationSubmenu(
        id: String,
        name: String,
        val pageCollection: FeaturedPageCollection,
        val width: FeaturedPageWidth,
        val layout: FeaturedPage
    ) : AbstractConfigurationSubmenu<FeaturedPageCollection>(id, name, pageCollection) {

        override fun LayoutScope.layout(modifier: Modifier) {
            val rows = layout.rows

            fun update(builder: MutableList<List<FeaturedItem>>.() -> Unit) {
                val mutableRows = rows.toMutableList()
                builder(mutableRows)
                currentlyEditing.update(currentlyEditing.copy(pages = currentlyEditing.pages + (width to layout.copy(rows = mutableRows))))
            }

            fun updateRow(rowIndex: Int, builder: MutableList<FeaturedItem>.() -> Unit) {
                val mutableRows = rows.map { it.toMutableList() }.toMutableList()
                val mutableRow = mutableRows[rowIndex]
                builder(mutableRow)
                currentlyEditing.update(currentlyEditing.copy(pages = currentlyEditing.pages + (width to layout.copy(rows = mutableRows))))
            }

            for ((rowIndex, row) in rows.withIndex()) {
                row(Modifier.fillWidth().height(10f).color(EssentialPalette.LIGHT_SCROLLBAR), Arrangement.SpaceBetween) {
                    text("Row " + (rowIndex + 1))
                    row(Modifier.fillHeight()) {
                        box(Modifier.widthAspect(1f).fillHeight()) {
                            icon(EssentialPalette.ARROW_UP_7X5)
                        }.onLeftClick {
                            if (rowIndex > 0) {
                                USound.playButtonPress()
                                update {
                                    removeAt(rowIndex)
                                    add(rowIndex - 1, row)
                                }
                            }
                        }
                        box(Modifier.widthAspect(1f).fillHeight()) {
                            icon(EssentialPalette.ARROW_DOWN_7X5)
                        }.onLeftClick {
                            if (rowIndex + 1 < rows.size) {
                                USound.playButtonPress()
                                update {
                                    removeAt(rowIndex)
                                    add(rowIndex + 1, row)
                                }
                            }
                        }
                        box(Modifier.widthAspect(1f).fillHeight()) {
                            icon(EssentialPalette.CANCEL_5X)
                        }.onLeftClick {
                            USound.playButtonPress()
                            update {
                                removeAt(rowIndex)
                            }
                        }
                    }
                }
                for ((itemIndex, item) in row.withIndex()) {
                    row(Modifier.fillWidth().height(10f), Arrangement.SpaceBetween) {
                        val text = "- " + when (item) {
                            is FeaturedItem.Bundle -> "Bundle: " + item.bundle
                            is FeaturedItem.Cosmetic -> "Cosmetic: " + item.cosmetic
                            is FeaturedItem.Empty -> "Empty"
                        }
                        text(text)
                        row(Modifier.fillHeight()) {
                            box(Modifier.widthAspect(1f).fillHeight()) {
                                icon(EssentialPalette.ARROW_UP_7X5)
                            }.onLeftClick {
                                if (itemIndex > 0) {
                                    USound.playButtonPress()
                                    updateRow(rowIndex) {
                                        removeAt(itemIndex)
                                        add(itemIndex - 1, item)
                                    }
                                }
                            }
                            box(Modifier.widthAspect(1f).fillHeight()) {
                                icon(EssentialPalette.ARROW_DOWN_7X5)
                            }.onLeftClick {
                                if (itemIndex + 1 < row.size) {
                                    USound.playButtonPress()
                                    updateRow(rowIndex) {
                                        removeAt(itemIndex)
                                        add(itemIndex + 1, item)
                                    }
                                }
                            }
                            box(Modifier.widthAspect(1f).fillHeight()) {
                                icon(EssentialPalette.CANCEL_5X)
                            }.onLeftClick {
                                USound.playButtonPress()
                                updateRow(rowIndex) {
                                    removeAt(itemIndex)
                                }
                            }
                        }
                    }
                    if (item is FeaturedItem.Cosmetic) {
                        val cosmeticId = item.cosmetic
                        val settings = mutableListStateOf(*item.settings.toTypedArray())
                        for (settingType in CosmeticSettingType.values()) {
                            val component = when (settingType) {
                                CosmeticSettingType.PLAYER_POSITION_ADJUSTMENT -> PlayerPositionAdjustmentSettingConfiguration(cosmeticsDataWithChanges, cosmeticId, settings)
                                CosmeticSettingType.SIDE -> SideSettingConfiguration(cosmeticsDataWithChanges, cosmeticId, settings)
                                CosmeticSettingType.VARIANT -> VariantSettingConfiguration(cosmeticsDataWithChanges, cosmeticId, settings)
                            }
                            component()
                        }
                        settings.onSetValue(stateScope) {
                            updateRow(rowIndex) {
                                this[itemIndex] = item.copy(settings = it)
                            }
                        }
                    }
                }
                row(Modifier.fillWidth(), Arrangement.spacedBy(10f, FloatPosition.END)) {
                    val bundleState = mutableStateOf<CosmeticBundle?>(null)
                    text("Add Bundle:")
                    essentialStateTextInput(
                        bundleState,
                        { it?.id ?: "" },
                        { if (it.isBlank()) null else (cosmeticsDataWithChanges.getCosmeticBundle(it) ?: throw StateTextInput.ParseException()) }
                    )
                    bundleState.onSetValue(stateScope) {
                        val bundleId = (it ?: return@onSetValue).id
                        updateRow(rowIndex) {
                            add(FeaturedItem.Bundle(bundleId))
                        }
                    }
                }
                row(Modifier.fillWidth(), Arrangement.spacedBy(10f, FloatPosition.END)) {
                    val cosmeticState = mutableStateOf<Cosmetic?>(null)
                    text("Add Cosmetic:")
                    essentialStateTextInput(
                        cosmeticState,
                        { it?.id ?: "" },
                        { if (it.isBlank()) null else (cosmeticsDataWithChanges.getCosmetic(it) ?: throw StateTextInput.ParseException()) }
                    )
                    cosmeticState.onSetValue(stateScope) {
                        val cosmeticId = (it ?: return@onSetValue).id
                        updateRow(rowIndex) {
                            add(FeaturedItem.Cosmetic(cosmeticId, listOf()))
                        }
                    }
                }
            }
            navButton("Add Row") {
                USound.playButtonPress()
                update {
                    add(listOf())
                }
            }
            val optionList = mutableListOf<EssentialDropDown.Option<FeaturedPageWidth?>>(EssentialDropDown.Option("", null))
            optionList += pageCollection.pages.keys.filter { it != width }.map { EssentialDropDown.Option("$it-wide", it) }

            if (optionList.size > 1) {
                divider()
                labeledListInputRow("Copy from page:", null, stateOf(optionList).toListState()) {
                    val featuredPage = pageCollection.pages[it] ?: return@labeledListInputRow // Should never happen
                    pageCollection.update(pageCollection.copy(pages = pageCollection.pages + (width to featuredPage)))
                }
            }
        }

    }

}
