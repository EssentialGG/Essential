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

import gg.essential.Essential
import gg.essential.elementa.components.ScrollComponent
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.divider
import gg.essential.gui.wardrobe.configuration.ConfigurationUtils.navButton
import gg.essential.util.*

sealed class AbstractConfiguration<I, T>(
    private val configurationType: ConfigurationType<I, T>,
    protected val state: WardrobeState,
) : LayoutDslComponent {

    protected val cosmeticsDataWithChanges = state.cosmeticsManager.cosmeticsDataWithChanges!!
    protected val referenceHolder = ReferenceHolderImpl()
    private val stateTriple = configurationType.stateSupplier(state)
    private val editingIdState = stateTriple.first
    private val editingState = stateTriple.second
    private val submenuMapState = editingState.map { editing -> if (editing != null) getSubmenus(editing).associateBy { it.id } else mapOf() }
    private val currentSubmenuId = mutableStateOf<String?>(null)
    private val editingItemAndSubmenu = stateBy {
        val currentlyEditing = editingState()
        val submenuMap = submenuMapState()
        val submenuId = currentSubmenuId()
        currentlyEditing to if (submenuId != null) submenuMap[submenuId] else null
    }

    private var savedScrollState: Pair<I, Float>? = null
    private var currentScrollComponent: ScrollComponent? = null

    override fun LayoutScope.layout(modifier: Modifier) {
        column(Modifier.fillParent().alignBoth(Alignment.Center), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
            bind(editingItemAndSubmenu) { (currentlyEditing, submenu) ->
                if (currentlyEditing != null) {
                    val (id, name) = currentlyEditing.idAndName()
                    column(Modifier.fillWidth().childBasedHeight(3f), Arrangement.spacedBy(3f, FloatPosition.CENTER)) {
                        text("Editing ${configurationType.displaySingular}")
                        text(name)
                        text("($id)")
                        if (submenu != null) {
                            text("Submenu: ${submenu.name}")
                        }
                    }
                    divider()
                    val savedScroll = savedScrollState
                    val scrollComponent = scrollable(Modifier.fillWidth().fillRemainingHeight(), vertical = true) {
                        column(Modifier.fillWidth(padding = 10f), Arrangement.spacedBy(3f)) {
                            spacer(height = 5f)
                            if (submenu != null) {
                                submenu()
                            } else {
                                columnLayout(currentlyEditing)
                            }
                            spacer(height = 5f)
                        }
                    }
                    currentScrollComponent = scrollComponent
                    if (savedScroll != null && savedScroll.first == currentlyEditing.id()) {
                        try {
                            scrollComponent.scrollTo(verticalOffset = savedScroll.second, smoothScroll = false)
                        } catch (e: Exception) {
                            Essential.logger.info("Prevented crash in AbstractConfiguration. (See EM-2304)", e)
                        }
                    }
                    divider()
                    row(Modifier.fillWidth().childBasedMaxHeight(3f), Arrangement.spacedBy(5f, FloatPosition.CENTER)) {
                        navButton("Reset", Modifier.fillWidth(0.3f)) {
                            GuiUtil.pushModal { manager -> getResetModal(manager, currentlyEditing) }
                        }
                        navButton("Delete", Modifier.fillWidth(0.3f)) {
                            GuiUtil.pushModal { manager -> getDeleteModal(manager, currentlyEditing) }
                        }
                        if (submenu != null) {
                            navButton("Back", Modifier.fillWidth(0.3f)) {
                                currentSubmenuId.set(null)
                            }
                        } else {
                            navButton("Close", Modifier.fillWidth(0.3f)) {
                                editingIdState.set(null)
                            }
                        }
                    }
                } else {
                    column(Modifier.fillRemainingHeight(), Arrangement.spacedBy(5f, FloatPosition.CENTER)) {
                        text("${configurationType.displaySingular} with id")
                        text("${editingIdState.get().toString()} not found")
                        navButton("Close") {
                            editingIdState.set(null)
                        }
                    }
                }
            }
        }
    }

    protected open fun LayoutScope.columnLayout(editing: T) {
        submenuSelection(editing)
    }

    protected fun LayoutScope.submenuSelection(editing: T) {
        val submenus = getSubmenus(editing)
        text(if (submenus.isEmpty()) "No submenus..." else "Select a submenu:")
        spacer(height = 10f)
        for (submenu in submenus) {
            navButton("Edit ${submenu.name}") {
                currentSubmenuId.set(submenu.id)
            }
        }
    }

    protected open fun getDeleteModal(modalManager: ModalManager, toDelete: T): Modal {
        return DangerConfirmationEssentialModal(modalManager, "Delete", false).configure {
            titleText = "Are you sure you want to delete ${configurationType.displaySingular} with id ${toDelete.id()}?"
        }.onPrimaryAction {
            toDelete.update(null)
            editingIdState.set(null)
        }
    }

    protected open fun getResetModal(modalManager: ModalManager, toReset: T): Modal {
        return DangerConfirmationEssentialModal(modalManager, "Reset", false).configure {
            titleText = "Are you sure you want to reset ${configurationType.displaySingular} with id ${toReset.id()} back to initial loaded state?"
        }.onPrimaryAction {
            toReset.reset()
        }
    }

    protected open fun getSubmenus(editing: T): Set<AbstractConfigurationSubmenu<T>> = setOf()

    protected fun T.update(newItem: T?) {
        val scrollComponent = currentScrollComponent
        if (scrollComponent != null) {
            savedScrollState = this.id() to scrollComponent.verticalOffset
        }
        configurationType.updateHandler(cosmeticsDataWithChanges, id(), newItem)
    }

    protected fun T.reset() = configurationType.resetHandler(cosmeticsDataWithChanges, id())

    protected fun T.idAndName() = configurationType.idAndNameMapper(this)

    protected fun T.id() = idAndName().first

    protected fun T.name() = idAndName().second

    protected sealed class AbstractConfigurationSubmenu<T>(val id: String, val name: String, val currentlyEditing: T) : LayoutDslComponent {

        abstract override fun LayoutScope.layout(modifier: Modifier)

    }

}
