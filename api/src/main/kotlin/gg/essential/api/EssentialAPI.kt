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

import gg.essential.api.commands.CommandRegistry
import gg.essential.api.config.EssentialConfig
import gg.essential.api.data.OnboardingData
import gg.essential.api.gui.EssentialComponentFactory
import gg.essential.api.gui.Notifications
import gg.essential.api.utils.*
import gg.essential.api.utils.mojang.MojangAPI
import gg.essential.elementa.components.image.ImageCache

/**
 * The central access point for all public Essential development tools. Certain API interfaces will have static
 * members for quick access, but they are all just "aliases" for the methods below. You can obtain an instance of
 * [EssentialAPI] via either [EssentialAPI.getInstance] or dependency injection (read more here [DI]).
 */
interface EssentialAPI {
    /**
     * The entry point to Essential's powerful Command API. All commands must be registered here if you wish for
     * them to work.
     */
    fun commandRegistry(): CommandRegistry

    /**
     * As said before, Essential provides the option of obtaining all of it's APIs via dependency injection (DI), as well
     * as providing a library for you to use DI in your own projects. Read more about Essential's DI system in [DI].
     */
    fun di(): DI

    /**
     * Notifications are a way to quickly display relevant information to the user without cluttering their chat
     * box, so Essential provides an easy to use API to display beautiful notifications.
     */
    fun notifications(): Notifications

    /**
     * Essential has some internal settings that players can modify with the Essential config GUI, and if you wish
     * to have behavior dependent on any of these options, you can access their values here.
     */
    fun config(): EssentialConfig


    /**
     * A collection of GUI utilities.
     */
    fun guiUtil(): GuiUtil

    /**
     * A collection of general Minecraft related utilities.
     */
    fun minecraftUtil(): MinecraftUtils

    /**
     * A utility that allows you run something when shutting down to prevent using [Runtime]'s shutdown hook.
     */
    fun shutdownHookUtil(): ShutdownHookUtil

    /**
     * Image cache for Minecraft skins.
     */
    fun imageCache(): ImageCache

    /**
     * Utility for interacting with Essential's trusted image host list.
     */
    fun trustedHostsUtil(): TrustedHostsUtil

    /**
     * Utility for using some of Essential's [Elementa](https://github.com/sk1erllc/elementa) components
     * in your guis.
     */
    fun componentFactory(): EssentialComponentFactory

    /**
     * Utility for interacting with the [Mojang API](https://wiki.vg/Mojang_API).
     */
    fun mojangAPI(): MojangAPI

    /**
     * Utility for accessing the player's Essential TOS status.
     */
    fun onboardingData(): OnboardingData

    companion object {
        private val instance: EssentialAPI = get()

        @JvmStatic
        fun getInstance(): EssentialAPI = instance

        @JvmStatic
        fun getCommandRegistry(): CommandRegistry = instance.commandRegistry()

        @JvmStatic
        fun getDI(): DI = instance.di()

        @JvmStatic
        fun getNotifications(): Notifications = instance.notifications()

        @JvmStatic
        fun getConfig(): EssentialConfig = instance.config()

        @JvmStatic
        fun getGuiUtil(): GuiUtil = instance.guiUtil()

        @JvmStatic
        fun getMinecraftUtil(): MinecraftUtils = instance.minecraftUtil()

        @JvmStatic
        fun getShutdownHookUtil() = instance.shutdownHookUtil()

        @JvmStatic
        fun getImageCache(): ImageCache = instance.imageCache()

        @JvmStatic
        fun getTrustedHostsUtil(): TrustedHostsUtil = instance.trustedHostsUtil()

        @JvmStatic
        fun getEssentialComponentFactory(): EssentialComponentFactory = instance.componentFactory()

        @JvmStatic
        fun getMojangAPI(): MojangAPI = instance.mojangAPI()

        @JvmStatic
        fun getOnboardingData(): OnboardingData = instance.onboardingData()
    }
}
