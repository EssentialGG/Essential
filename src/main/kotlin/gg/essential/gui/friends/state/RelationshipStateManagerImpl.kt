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
package gg.essential.gui.friends.state

import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.EssentialPalette
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.iconAndMarkdownBody
import gg.essential.network.connectionmanager.relationship.FriendRequestState
import gg.essential.network.connectionmanager.relationship.RelationshipManager
import gg.essential.network.connectionmanager.relationship.RelationshipResponse
import gg.essential.util.*
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

class RelationshipStateManagerImpl(
    private val relationshipManager: RelationshipManager
) : IRelationshipStates, IRelationshipManager {

    private val observableFriendList = ObservableList(relationshipManager.friends.keys.toMutableList())
    private val observableBlockedList = ObservableList(relationshipManager.blockedByMe.keys.toMutableList())
    private val observableIncomingList = ObservableList(relationshipManager.incomingFriendRequests.keys.toMutableList())
    private val observableOutgoingList = ObservableList(relationshipManager.outgoingFriendRequests.keys.toMutableList())

    init {
        relationshipManager.registerStateManager(this)
    }

    /** IRelationshipStates **/

    override fun getObservableFriendList(): ObservableList<UUID> {
        return observableFriendList
    }

    override fun getObservableBlockedList(): ObservableList<UUID> {
        return observableBlockedList
    }

    override fun getObservableIncomingRequests(): ObservableList<UUID> {
        return observableIncomingList
    }

    override fun getObservableOutgoingRequests(): ObservableList<UUID> {
        return observableOutgoingList
    }

    override fun addFriend(uuid: UUID, notification: Boolean): CompletableFuture<RelationshipResponse> {
        val future = relationshipManager.addFriend(uuid, notification)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { username ->
                Notifications.push("", "") {
                    iconAndMarkdownBody(
                        EssentialPalette.ENVELOPE_9X7.create(),
                        "Friend request sent to ${username.colored(EssentialPalette.TEXT_HIGHLIGHT)}"
                    )
                }
            }
        }
        return future
    }

    override fun removeFriend(uuid: UUID, notification: Boolean) {
        val future = relationshipManager.removeFriend(uuid)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { username ->
                Notifications.push("", "") {
                    iconAndMarkdownBody(
                        EssentialPalette.REMOVE_FRIEND_PLAYER_10X7.create(),
                        "${username.colored(EssentialPalette.TEXT_HIGHLIGHT)} is no longer your friend"
                    )
                }
            }
        }

    }

    override fun acceptIncomingFriendRequest(uuid: UUID, notification: Boolean) {
        val future = relationshipManager.acceptFriend(uuid)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { username ->
                Notifications.push("", "") {
                    iconAndMarkdownBody(
                        CachedAvatarImage.create(uuid),
                        "${username.colored(EssentialPalette.TEXT_HIGHLIGHT)} accepted your friend request",
                    )
                }
            }
        }
    }

    override fun blockPlayer(uuid: UUID, notification: Boolean): CompletableFuture<RelationshipResponse> {
        val future = relationshipManager.createBlockedRelationship(uuid, notification)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { username ->
                Notifications.push("", "") {
                    iconAndMarkdownBody(
                        EssentialPalette.BLOCK_7X7.create(),
                        "${username.colored(EssentialPalette.TEXT_HIGHLIGHT)} has been blocked"
                    )
                }
            }
        }
        return future
    }

    override fun unblockPlayer(uuid: UUID, notification: Boolean) {
        val future = relationshipManager.unblock(uuid)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { }
        }
    }

    override fun declineIncomingFriendRequest(uuid: UUID, notification: Boolean) {
        val future = relationshipManager.denyFriend(uuid)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { }
        }
    }

    override fun cancelOutgoingFriendRequest(uuid: UUID, notification: Boolean) {
        val future = relationshipManager.cancelFriendRequest(uuid)
        if (notification) {
            consumeRelationshipFuture(uuid, future) { }

        }
    }

    override fun getPendingRequestTime(uuid: UUID): Instant? {
        val relationship =
            relationshipManager.getIncomingFriendRequest(uuid) ?: relationshipManager.getOutgoingFriendRequest(uuid)
            ?: return null
        return relationship.since.toInstant()
    }

    private fun consumeRelationshipFuture(
        uuid: UUID,
        future: CompletableFuture<RelationshipResponse>,
        onSuccess: (String) -> Unit,
    ) {
        future.thenAcceptOnMainThread {
            when (it.friendRequestState) {
                FriendRequestState.SENT -> {
                    UUIDUtil.getName(uuid).thenAcceptOnMainThread { username ->
                        onSuccess(username)
                    }
                }

                FriendRequestState.ERROR_UNHANDLED -> {
                    if (it.relationshipErrorResponse == null) {
                        Notifications.error("Error", it.message ?: "")
                    } else {
                        it.displayToast(uuid)
                    }
                }
                FriendRequestState.ERROR_HANDLED -> {}
            }
        }
    }

    /** IRelationshipCallbacks **/

    override fun friendAdded(uuid: UUID) {
        observableFriendList.add(uuid)
    }

    override fun friendRemoved(uuid: UUID) {
        observableFriendList.remove(uuid)
    }

    override fun clearFriends() {
        observableFriendList.clear()
    }

    override fun playerBlocked(uuid: UUID) {
        observableBlockedList.add(uuid)
    }

    override fun playerUnblocked(uuid: UUID) {
        observableBlockedList.remove(uuid)
    }

    override fun clearBlocked() {
        observableBlockedList.clear()
    }

    override fun newIncomingFriendRequest(uuid: UUID) {
        observableIncomingList.add(uuid)
    }

    override fun clearIncomingFriendRequest(uuid: UUID) {
        observableIncomingList.remove(uuid)
    }

    override fun clearAllIncomingRequests() {
        observableIncomingList.clear()
    }

    override fun newOutgoingFriendRequest(uuid: UUID) {
        observableOutgoingList.add(uuid)
    }

    override fun clearOutgoingFriendRequest(uuid: UUID) {
        observableOutgoingList.remove(uuid)
    }

    override fun clearAllOutgoingRequests() {
        observableOutgoingList.clear()
    }
}
