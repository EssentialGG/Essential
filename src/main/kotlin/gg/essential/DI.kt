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
package gg.essential

import gg.essential.api.EssentialAPI
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.provider
import org.kodein.di.singleton

object DI : gg.essential.api.DI() {
    private val modules: MutableList<DI.Module> = mutableListOf()
    private val logger: Logger = LogManager.getLogger("Essential - DI")
    private var hasStarted = false

    fun startDI() {
        if (hasStarted) {
            logger.error("Essential Dependency Injection attempted to start, but has already started.")
            return
        }

        logger.info("Starting DI!")
        init()

        hasStarted = true
    }

    override fun addModule(module: DI.Module) {
        modules.add(module)
    }

    override val di: DI
        get() = DI {
            bind<EssentialAPI>() with provider { Essential.getInstance() }

            singleton { Essential.getInstance().commandRegistry() }
            singleton { Essential.getInstance().di() }
            singleton { Essential.getInstance().notifications() }
            singleton { Essential.getInstance().config() }
            singleton { Essential.getInstance().guiUtil() }
            singleton { Essential.getInstance().minecraftUtil() }
            singleton { Essential.getInstance().imageCache() }
            singleton { Essential.getInstance().trustedHostsUtil() }
            singleton { Essential.getInstance().onboardingData() }

            // modules loaded through essential api
            importAll(modules)
        }
}
