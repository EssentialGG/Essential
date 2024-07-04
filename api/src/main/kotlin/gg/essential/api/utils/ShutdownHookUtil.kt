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

/**
 * Utility for running code when the game is closed.
 */
interface ShutdownHookUtil {
    /**
     * Register a [Runnable] to be run when the game is closed.
     *
     * @param task task to be run
     */
    fun register(task: Runnable)

    /**
     * Unregister a previously registered [Runnable].
     *
     * @param task task to be unregistered
     */
    fun unregister(task: Runnable)
}