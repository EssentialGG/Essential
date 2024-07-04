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
package gg.essential.api

import gg.essential.api.utils.essentialDI
import org.kodein.di.DIAware

/**
 * Essential uses Dependency Injection, or DI for short, to provide instances of its API interfaces. The library
 * [Kodein](https://kodein.org/di/) is used to power our DI system, so feel free to read their documentation
 * to familiarize yourself with its usage, though simple usage is explained below.
 *
 * If you wish to access these instances via DI rather than the [EssentialAPI] getters, you can use the
 * [gg.essential.api.utils.get] function from Kotlin like so: `val hud = get<HudRegistry>()`.
 *
 * If you wish to customize Essential's DI by making your own types available and usable in your project, use [addModule].
 */
abstract class DI : DIAware {
    /**
     * To add your own types to Essential's DI engine, simply provide a [org.kodein.di.DI.Module] instance.
     * You can find information about creating these modules in their
     * [documentation](https://docs.kodein.org/kodein-di/7.6/core/modules-inheritance.html).
     */
    abstract fun addModule(module: org.kodein.di.DI.Module)

    protected fun init() {
        essentialDI = this
    }
}
