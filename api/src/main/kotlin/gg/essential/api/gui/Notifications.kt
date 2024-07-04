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
package gg.essential.api.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.State
import java.util.*

/**
 * Simple API for notifying the user of some information.
 *
 * The notifications show up in the bottom-right in-game, sliding out from the right side of the screen,
 * similarly to the Windows 10 notification style. Notifications last for approximately 4 seconds, unless
 * hovered by the user, at which point that timer will stop until they move their mouse away from the notification.
 *
 * Notifications are styled with the [gg.essential.vigilance.gui.VigilancePalette] color scheme.
 */
interface Notifications {
    /**
     * Push a new, non-interactive notification with the given title and message.
     *
     * Since no callback is provided, the notification will simply go away when clicked, and no further
     * action will be taken.
     *
     * @param title notification header
     * @param message notification body
     */
    fun push(title: String, message: String)

    /**
     * Push a new notification with the given title and message.
     *
     * Further details may be configured via a [NotificationBuilder] by passing a [configure] function.
     *
     * @param title notification header
     * @param message notification body
     * @param configure a function to prepare a NotificationBuilder to modify the notification as desired
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    fun pushWithBuilder(title: String, message: String, configure: NotificationBuilder.() -> Unit = {})

    /**
     * Push a new, non-interactive notification with the given title, message, and a customizable duration.
     *
     * Since no callback is provided, the notification will simply go away when clicked, and no further
     * action will be taken.
     *
     * @param title notification header
     * @param message notification body
     * @param duration how long in seconds the notification will stay on screen
     */
    fun push(title: String, message: String, duration: Float = 4f)

    /**
     * Push a new notification with the given title and message, as well as [action] which will be invoked
     * when the user clicks on the notification. This can be used for all sorts of purposes, as it is generally useful
     * to respond with some type of action when the user clicks a notification.
     *
     * @param title notification header
     * @param message notification body
     * @param action ran when the player clicks the notification
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    fun pushWithAction(title: String, message: String, action: () -> Unit = {})

    /**
     * Push a new notification with the given title, message, customizable duration, as well as [action] which will be invoked
     * when the user clicks on the notification. This can be used for all sorts of purposes, as it is generally useful
     * to respond with some type of action when the user clicks a notification.
     *
     * @param title notification header
     * @param message notification body
     * @param duration how long in seconds the notification will stay on screen
     * @param action ran when the player clicks the notification
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    fun pushWithDurationAndAction(title: String, message: String, duration: Float = 4f, action: () -> Unit = {})

    /**
     * Push a new notification with the given title, message, customizable duration, [action] which will be invoked
     * when the user clicks on the notification, and [close] which will be invoked when the notification has expired.
     * This can be used for all sorts of purposes, as it is generally useful
     * to respond with some type of action when the user clicks a notification.
     *
     * @param title notification header
     * @param message notification body
     * @param duration how long in seconds the notification will stay on screen
     * @param action ran when the player clicks the notification
     * @param close ran when the notification has expired
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    fun pushWithDurationActionAndClose(title: String, message: String, duration: Float = 4f, action: () -> Unit = {}, close: () -> Unit = {})

    /**
     * Push a new notification with the given title, message, customizable duration, [action] which will be invoked
     * when the user clicks on the notification, and [close] which will be invoked when the notification has expired.
     * This can be used for all sorts of purposes, as it is generally useful
     * to respond with some type of action when the user clicks a notification.
     *
     * @param title notification header
     * @param message notification body
     * @param duration how long in seconds the notification will stay on screen
     * @param action ran when the player clicks the notification
     * @param close ran when the notification has expired
     * @param configure a function to prepare a NotificationBuilder to modify the notification as desired
     */
    fun push(
        title: String,
        message: String,
        duration: Float = 4f,
        action: () -> Unit = {},
        close: () -> Unit = {},
        configure: NotificationBuilder.() -> Unit = {}
    )
}

interface NotificationBuilder {
    /**
     * How long in seconds the notification will stay on screen.
     * Default value is 4 seconds.
     */
    var duration: Float

    /**
     * Callback to be invoked when the user clicks on the notification.
     * This can be used for all sorts of purposes, as it is generally useful to respond with some type of action when
     * the user clicks a notification.
     */
    var onAction: () -> Unit

    /**
     * Callback to be invoked once the notification is closed.
     * This will be invoked even when [action] was already invoked (in such cases, [action] will be invoked first).
     */
    var onClose: () -> Unit

    var elementaVersion: ElementaVersion

    /**
     * State controlling whether the notification timer is progressing or not
     */
    var timerEnabled: State<Boolean>

    /**
     * Whether the notification's title should be cut off at the first line or not
     */
    var trimTitle: Boolean

    /**
     * Whether the notification's message should be cut off at 3 lines or not
     */
    var trimMessage: Boolean

    /**
     * The color to be used for the headline of the notification.
     */
    var type: NotificationType

    /**
     * The unique id of the notification, used to prevent duplicates by disregarding new notifications while
     * the current notification is still active.
     *
     * The given object must have a correct and stable `hashCode` and `equals` implementation, using which it is compared to other ids.
     *
     * Hint: A plain `static Object MY_ID = new Object();` (or for Kotlin a plain `object MyId`) does fulfill these requirements
     * and as such makes for a great unique id for most simple cases.
     *
     * Hint: An even simpler option (provided you only need it in a single place) is using an anonymous class:
     * `new Object(){}.getClass()` (or for Kotlin: `object {}.javaClass`). Note that you need to pass the anonymous class,
     * which is a singleton; you must not just pass an instance of that class, because that will be a different Object on each invocation.
     *
     * Note that if you use a String (or any other public type) as the id, that String must be unique across all mods.
     * The recommended way to guarantee this is to prefix your string with your mod id, separated with a colon,
     * e.g. `mymodid:mynotification`, or to wrap it into a custom `data class` which only your mod uses.
     */
    var uniqueId: Any?

    /**
     * A function that dismisses the notification when called. The notification will animate out and then be removed.
     */
    val dismissNotification: () -> Unit

    /**
     * A function that dismisses the notification instantly when called. The notification will be removed without animating out.
     */
    val dismissNotificationInstantly: () -> Unit

    fun withElementaVersion(version: ElementaVersion) = apply { this.elementaVersion = version }

    fun withCustomComponent(slot: Slot, component: UIComponent): NotificationBuilder
}

enum class Slot {
    ACTION,
    LARGE_PREVIEW,
    SMALL_PREVIEW,

    @Deprecated("Replaced by `ICON` (same position as `PREVIEW`; meant for small icons) and `SMALL_PREVIEW` (similar position as `ACTION`; meant for small to mid sized previews)")
    PREVIEW,

    ICON,
}

enum class NotificationType {
    GENERAL,
    INFO,
    WARNING,
    ERROR,
    DISCORD,
}
