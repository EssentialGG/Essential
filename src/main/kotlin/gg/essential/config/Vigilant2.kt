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

import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.combinators.bimap
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.vigilancev2.builder.StateBackedPropertyValue
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Migration
import gg.essential.vigilance.data.PropertyAttributesExt
import gg.essential.vigilance.data.PropertyCollector
import gg.essential.vigilance.data.PropertyData
import gg.essential.vigilance.data.PropertyType
import java.awt.Color
import java.io.File

open class Vigilant2 {
    private val referenceHolder = ReferenceHolderImpl()
    private val propertyCollector = LateBindPropertyCollector()
    private var backend: Vigilant? = null

    fun initialize(file: File) {
        if (backend != null) {
            throw IllegalStateException("Already initialized.")
        }
        backend = object : Vigilant(file, "", propertyCollector) {
            override val migrations: List<Migration>
                get() = this@Vigilant2.migrations
        }.apply { initialize() }
    }

    fun <T> property(path: String, type: PropertyType, defaultValue: T): MutableState<T> {
        val state = mutableStateOf(defaultValue)
        if ("." !in path) {
            throw IllegalArgumentException("The old Vigilance backend requires a dot-separated category in the path.")
        }
        val (category, name) = path.split(".", limit = 2)
        val attributes = PropertyAttributesExt(type, name, category, hidden = true)
        propertyCollector.lateBindProperties.add { instance ->
            state.onSetValue(referenceHolder) { instance.markDirty() }
            PropertyData(attributes, StateBackedPropertyValue(state, true), instance)
        }
        return state
    }

    //region type-specific property() overloads

    fun property(path: String, defaultValue: Boolean): MutableState<Boolean> = property(path, PropertyType.SWITCH, defaultValue)

    fun property(path: String, defaultValue: String): MutableState<String> = property(path, PropertyType.TEXT, defaultValue)

    fun property(path: String, defaultValue: Int): MutableState<Int> = property(path, PropertyType.NUMBER, defaultValue)

    fun property(path: String, defaultValue: Float): MutableState<Float> = property(path, PropertyType.DECIMAL_SLIDER, defaultValue)

    fun property(path: String, defaultValue: Color): MutableState<Color> = property(path, PropertyType.COLOR, defaultValue)

    inline fun <reified T : Enum<T>> property(path: String, defaultValue: T): MutableState<T> =
        property(path, PropertyType.SELECTOR, defaultValue.ordinal)
            .bimap({ T::class.java.enumConstants[it] }, { it.ordinal })

    //endregion

    protected fun buildGui(title: String, block: GuiBuilder.() -> Unit): GuiFactory =
        GuiBuilder.build(title, block)

    protected fun lazyBuildGui(title: String, block: GuiBuilder.() -> Unit): Lazy<GuiFactory> =
        lazy { buildGui(title, block) }

    open val migrations: List<Migration> = emptyList()

    private class LateBindPropertyCollector : PropertyCollector() {
        val lateBindProperties = mutableListOf<(Vigilant) -> PropertyData>()

        override fun collectProperties(instance: Vigilant): List<PropertyData> {
            return lateBindProperties.map { it(instance) }
        }
    }
}
