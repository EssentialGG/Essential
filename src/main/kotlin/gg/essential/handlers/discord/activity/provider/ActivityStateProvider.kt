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
package gg.essential.handlers.discord.activity.provider

import gg.essential.handlers.discord.activity.ActivityState

/**
 * An ActivityStateProvider does what it says on the tin, it provides an activity state when queried for one.
 */
interface ActivityStateProvider {
    /**
     * Called when the Discord Integration is initializing, do any event listener registration here.
     */
    fun init() {}

    /**
     * Called when the Discord Integration wants an activity state for your provider
     * If you do not have an activity state at this time, just return null.
     *
     * Implementations of this method **MUST** be thread-safe because they are called from
     * a background thread.
     */
    fun provide(): ActivityState?
}