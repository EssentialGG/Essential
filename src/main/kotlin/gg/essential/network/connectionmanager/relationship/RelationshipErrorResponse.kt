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
package gg.essential.network.connectionmanager.relationship

import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.markdownBody
import gg.essential.gui.notification.warning
import gg.essential.util.colored
import net.minecraft.client.resources.I18n
import java.util.*

enum class RelationshipErrorResponse(
    val showToast: (UUID, String) -> Unit
) {

    EXISTING_REQUEST_IS_PENDING({ _, name ->
        Notifications.error("Friend request failed", "") {
            markdownBody("Friend request already sent to ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}.")
        }
    }),

    SENDER_TYPE_VERIFIED_SLOT_LIMIT({ _, _ ->
        Notifications.error(
            "Friend request failed",
            "Your friends list is full. Remove friends before adding new ones."
        )
    }),
    TARGET_TYPE_VERIFIED_SLOT_LIMIT({ _, name ->
        Notifications.error("Friend request failed", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}'s friends list is full.")
        }
    }),
    NO_PENDING_REQUEST_TO_VERIFY({ _, _ ->
        Notifications.error("Error", "No pending friend request to accept.")
    }),
    VERIFIED_RELATIONSHIP_ALREADY_EXISTS({ _, name ->
        Notifications.error("Friend request failed", "") {
            markdownBody("You are already friends with ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}.")
        }
    }),
    NO_BLOCKED_RELATIONSHIP_TO_CONVERT({ _, name ->
        Notifications.error("Error", "") {
            markdownBody("Failed to block ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}.")
        }
    }),
    SENDER_BLOCKED_TARGET({ _, name ->
        Notifications.error("Friend request failed", "") {
            markdownBody("You have blocked ${name.colored(EssentialPalette.TEXT_HIGHLIGHT)}. " +
                    "Unblock them before sending them a friend request.")
        }
    }),
    TARGET_BLOCKED_SENDER({ _, name ->
        Notifications.warning("Friend request declined", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} has blocked you.")
        }
    }),
    SENDER_TYPE_OUTGOING_SLOT_LIMIT({ _, _ ->
        Notifications.error(
            "Friend request failed",
            "You have too many pending friend requests. Remove some to make space for new ones."
        )
    }),
    TARGET_TYPE_INCOMING_SLOT_LIMIT({ _, name ->
        Notifications.error("Friend request failed", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} has too many pending friend requests.")
        }
    }),
    TARGET_PRIVACY_SETTING_FRIEND_OF_FRIENDS({ _, name ->
        Notifications.warning("Friend request declined", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} is only accepting friend requests from friends of friends.")
        }
    }),
    TARGET_PRIVACY_SETTING_NO_ONE({ _, name ->
        Notifications.warning("Friend request declined", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} is not accepting any friend requests.")
        }
    }),
    TARGET_NOT_EXIST({ _, name ->
        Notifications.error("Error", "") {
            markdownBody("${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} does not exist.")
        }
    });

    val message: String
        get() = I18n.format("connectionmanager.friends." + name)

    companion object {
        @JvmStatic
        fun getResponse(response: String): RelationshipErrorResponse? {
            return try {
                valueOf(response)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
