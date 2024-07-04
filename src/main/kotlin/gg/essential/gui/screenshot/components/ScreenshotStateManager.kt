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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.network.connectionmanager.media.ScreenshotManager
import gg.essential.util.Multithreading

/**
 * Manages states relating to the screenshot browser
 */
class ScreenshotStateManager(
    private val screenshotManager: ScreenshotManager,
) {

    private val favoriteMap = mutableMapOf<ScreenshotProperties, State<Boolean>>()
    private val aspectMap = mutableMapOf<ScreenshotProperties, State<Float>>()

    /**
     * Creates a State<Boolean> that stores whether the screenshot in [properties] is favorite.
     * Updates to the state will be automatically forwarded to the screenshot manager
     */
    private fun registerFavoriteState(properties: ScreenshotProperties, state: State<Boolean>) {
        state.onSetValue {
            //On a separate thread because it involves IO operations
            Multithreading.runAsync {
                properties.metadata =
                    when (val id = properties.id) {
                        is LocalScreenshot ->
                            screenshotManager.setFavorite(id.path, it)
                        is RemoteScreenshot ->
                            screenshotManager.setFavorite(id.media, it)
                    }
            }
        }
    }

    /**
     * Gets the cached aspect ratio of this screenshot.
     * This method will NOT load the aspect ratio if it's not already loaded.
     * That behavior is done in the callbacks from [ScreenshotProviderManager] in [FocusListComponent] and [ListViewComponent]
     */
    fun getAspectRatio(properties: ScreenshotProperties): State<Float> {
        return aspectMap.computeIfAbsent(properties) {
            BasicState(16 / 9f)
        }
    }

    /**
     * Gets the current favorite state for the screenshot in [properties].
     * Updates to this state will be automatically forwarded to the [gg.essential.network.connectionmanager.media.ScreenshotManager]
     * to be persisted
     */
    fun getFavoriteState(properties: ScreenshotProperties): State<Boolean> {
        return favoriteMap.computeIfAbsent(properties) {
            BasicState(it.metadata?.favorite ?: false).also { state ->
                registerFavoriteState(it, state)
            }
        }
    }

    /**
     * Gets a text state that is returns the string that should be displayed
     * for the favorite action
     */
    fun getFavoriteTextState(properties: ScreenshotProperties): State<String> {
        return mapFavoriteText(getFavoriteState(properties))
    }

    fun mapFavoriteText(favorite: State<Boolean>): State<String> {
        return favorite.map {
            if (it) {
                "Remove Favorite"
            } else {
                "Favorite"
            }
        }
    }

    fun handleDelete(properties: ScreenshotProperties) {
        aspectMap.remove(properties)
        favoriteMap.remove(properties)
    }


}