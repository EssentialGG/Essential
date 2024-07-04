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
package gg.essential.handlers.discord.activity

import gg.essential.config.EssentialConfig

/**
 * The different 'activity states' that can be sent when using the Discord Integration
 */
sealed class ActivityState {
    /**
     * The text which is shown under "Playing Minecraft" on the user's Discord Presence
     */
    abstract val text: String

    /**
     * Shown when the user is playing on a Multiplayer server
     *
     * @param address The IP address of the server (e.g. mc.hypixel.net)
     */
    data class Multiplayer(
        private val address: String,
    ) : ActivityState() {
        override val text = "Playing on ${if (EssentialConfig.discordShowCurrentServer) address else "a server"}"
    }

    /**
     * Shown when the user is hosting an SPS session on single player
     *
     * @param partyInfo The information about the party
     */
    object SPSHost : ActivityState() {
        override val text = "Hosting a world"
    }

    /**
     * Shown when the user is a guest of a single player session
     *
     * @param hostUsername The username of the host of the SPS session
     */
    data class SPSGuest(
        private val hostUsername: String,
    ) : ActivityState() {
        override val text = "Playing on $hostUsername's world"
    }

    /**
     * Shown when the user is in a single player world
     */
    object Singleplayer : ActivityState() {
        override val text = "Playing Singleplayer"
    }

    /**
     * Shown when the user is connected to a realm
     */
    object Realm : ActivityState() {
        override val text: String = "Playing on a realm"
    }

    object GUI {
        /**
         * Shown when the user is on an [gg.essential.api.gui.EssentialGUI]
         */
        data class Described(override val text: String) : ActivityState()

        /**
         * Shown when the user is, well on the main menu
         */
        object MainMenu : ActivityState() {
            override val text = "Looking at the main menu"
        }

        /**
         * Shown when the user is in the 'Multiplayer' screen, or any of its children (i.e. adding a server)
         */
        object ServerList : ActivityState() {
            override val text = "Selecting server"
        }

        /**
         * Shown when the user is in a Settings-related screen
         *
         * @param vanilla If the settings screen is the Minecraft options screen or not
         */
        data class Options(private val vanilla: Boolean = true) : ActivityState() {
            override val text = "Configuring ${if (vanilla) "minecraft " else ""}settings"
        }
    }
}
