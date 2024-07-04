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
package gg.essential.api.config

/**
 * Options from Essential's `Essential Settings` menu.
 */
interface EssentialConfig {
    /**
     * Show [Notifications] when a friend come online.
     */
    var friendConnectionStatus: Boolean

    /**
     * Show the option to open singleplayer worlds to friends.
     */
    var openToFriends: Boolean

    /**
     * Block all of Essential's [Notifications].
     */
    var disableAllNotifications: Boolean

    /**
     * Show [Notifications] when the player receives a direct message.
     */
    var messageReceivedNotifications: Boolean

    /**
     * Show [Notifications] when the player receives a message from a group.
     */
    var groupMessageReceivedNotifications: Boolean

    /**
     * Play a sound when the player receives a message.
     */
    var messageSound: Boolean

    /**
     * Display a notification modal after Essential has updated.
     */
    var updateModal: Boolean

    /**
     * Show an icon in the tab list on players using Essential.
     */
    var showEssentialIndicatorOnTab: Boolean

    /**
     * Show an icon on the name tags of players using Essential.
     */
    var showEssentialIndicatorOnNametag: Boolean

    /**
     * Share the player's current server with friends.
     */
    var sendServerUpdates: Boolean


    /**
     * The player's friend request privacy level.
     *
     * 0 = Anyone can send the player friend requests;
     * 1 = Only friends of friends can send the player friend requests;
     * 2 = Nobody can send the player friend requests.
     */
    var friendRequestPrivacy: Int

    /**
     * The multiplayer tab the player last used.
     *
     * 0 = Favourite servers;
     * 1 = Servers with friends on them;
     * 2 = Discover servers.
     */
    var currentMultiplayerTab: Int

    /**
     * Show the player a warning popup when they are using ModCore.
     */
    var modCoreWarning: Boolean

    /**
     * Show the player a confirmation modal before opening a link to a trusted host.
     */
    var linkWarning: Boolean

    /**
     * Use the cinematic camera when zooming.
     */
    var zoomSmoothCamera: Boolean

    /**
     * Animate zooming in.
     */
    var smoothZoomAnimation: Boolean

    @Deprecated("Removed.")
    var smoothZoomAlgorithm: Int

    /**
     * Zoom key toggles zoom (instead of the player having to hold down the key).
     */
    var toggleToZoom: Boolean

    /**
     * Player's selected Essential "mode".
     *
     * True for Essential Full, false for Essential Mini.
     */
    @Deprecated("This setting no longer has any effect")
    var essentialFull: Boolean

    /**
     * Choose the size of all Essential Menus.
     */
    @Deprecated("This setting no longer has any effect, will now always be auto (0)")
    var essentialGuiScale: Int

    /**
     * Automatically refresh the active session if it's expired when connecting to a server.
     */
    var autoRefreshSession: Boolean

    //#if MC<11400
    /**
     * Use a borderless version of fullscreen.
     */
    var windowedFullscreen: Boolean
    //#endif

    /**
     * Disable all [Notifications] and notification sounds.
     */
    var streamerMode: Boolean

    @Deprecated("This setting no longer has any effect")
    var discordSdk: Boolean

    /**
     * Enables Discord Rich Presence.
     */
    var discordRichPresence: Boolean

    /**
     * Enables Discord's Ask To Join.
     */
    var discordAllowAskToJoin: Boolean

    /**
     * Shows the user's username and avatar on the rich presence
     */
    var discordShowUsernameAndAvatar: Boolean

    /**
     * Shows the server that the user is connected to on their rich presence
     */
    var discordShowCurrentServer: Boolean

    /**
     * When enabled and the server overrides a skin, all cosmetics will be hidden on that player
     * This is for game-modes such as Hypixel murder mystery where having a suit equipped can lead to an advantage
     */
    var hideCosmeticsWhenServerOverridesSkin: Boolean

    /**
     * Enable Essential's screenshot manager.
     */
    var essentialScreenshots: Boolean

    /**
     * Play a sound when capturing a screenshot
     */
    var screenshotSounds: Boolean


    /**
     * Whether the vanilla screenshot message is sent in chat on capture
     */
    var enableVanillaScreenshotMessage: Boolean

    @Deprecated("This setting is no longer used")
    var cosmeticArmorSetting: Int

    /**
     * Choose between using 12 or 24 hour time for dates/timestamps.
     * 0 = 12 hour time (03:00 AM, 03:00 PM)
     * 1 = 24 hour time (03:00, 15:00)
     */
    var timeFormat: Int

    @Deprecated(
        message = "No longer used, replaced by essentialGuiScale.",
        replaceWith = ReplaceWith("essentialGuiScale")
    )
    var overrideGuiScale: Boolean
}
