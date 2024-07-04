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
package gg.essential.api.utils

import org.apache.logging.log4j.LogManager
import org.kodein.di.direct
import org.kodein.di.instance

/**
 * Utility to quickly retrieve an instance from our dependency injection framework. The generic type
 * [T] is the class that is looked up.
 *
 * @see gg.essential.api.DI
 */
inline fun <reified T : Any> get(): T = essentialDI!!.direct.instance()

/**
 * This will be true if Essential's DI has been initialised.
 */
var initialised: Boolean = false
    private set

/**
 * Gets an instance of Essential's DI. Try not to call this directly and instead use [get].
 */
var essentialDI: gg.essential.api.DI? = null
    internal set(value) {
        if (initialised) {
            LogManager.getLogger("Essential - DI").error("DI already set!")
        } else {
            field = value
            initialised = true
        }
    }
    get() {
        if (initialised) {
            return field
        }
        throw RuntimeException("DI not initialised!")
    }
