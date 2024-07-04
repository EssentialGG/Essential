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

import gg.essential.api.EssentialAPI
import net.minecraft.client.gui.GuiScreen

/**
 * A collection of simple & handy utility functions for interacting with Minecraft's GUI system.
 */
interface GuiUtil {
    /**
     * Queue a new screen for opening. This API will make sure the GUI will be displayed synchronously,
     * avoiding any weird mouse glitches.
     */
    fun openScreen(screen: GuiScreen?)

    /**
     * @return the currently open screen, or null if none is opened
     */
    fun openedScreen(): GuiScreen?

    /**
     * @return -1 for current MC gui scale or positive integer indicating the GUI scale
     */
    fun getGuiScale(): Int

    /**
     * @return -1 for current MC gui scale or positive integer indicating the GUI scale
     */
    fun getGuiScale(step: Int): Int

    companion object {
        @JvmStatic
        fun open(screen: GuiScreen?) = EssentialAPI.getGuiUtil().openScreen(screen)

        @JvmStatic
        fun getOpenedScreen() = EssentialAPI.getGuiUtil().openedScreen()
    }
}
