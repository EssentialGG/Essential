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
package gg.essential.network.connectionmanager.notices

import gg.essential.elementa.state.BasicState
import gg.essential.gui.common.WeakState
import gg.essential.notices.NoticeType
import gg.essential.notices.model.Notice
import java.util.UUID

class SocialMenuNewFriendRequestNoticeManager(
    private val noticesManager: NoticesManager,
) : NoticeListener {

    private val unseenFriendRequests = mutableMapOf<UUID, Notice>()
    private val unseenFriendRequestStates = mutableMapOf<UUID, BasicState<Boolean>>()
    private val unseenFriendRequestCount = BasicState(0)

    override fun noticeAdded(notice: Notice) {
        if (notice.type != NoticeType.FRIEND_REQUEST_NEW_INDICATOR) {
            return
        }
        val uuid = UUID.fromString(notice.metadata["uuid"] as? String ?: return)
        unseenFriendRequests[uuid] = notice
        unseenFriendRequestStates.getOrPut(uuid) { BasicState(false) }.set { true }
        unseenFriendRequestCount.set(unseenFriendRequests.size)
    }

    override fun noticeRemoved(notice: Notice) {
        if (notice.type != NoticeType.FRIEND_REQUEST_NEW_INDICATOR) {
            return
        }
        val uuid = UUID.fromString(notice.metadata["uuid"] as? String ?: return)
        unseenFriendRequests.remove(uuid)
        unseenFriendRequestStates[uuid]?.set { false }
        unseenFriendRequestCount.set(unseenFriendRequests.size)
    }

    fun hasUnseenFriendRequests(uuid: UUID): WeakState<Boolean> {
        return WeakState(unseenFriendRequestStates.getOrPut(uuid) { BasicState(false) })
    }

    fun clearUnseenFriendRequests(uuid: UUID) {
        val notice = unseenFriendRequests.remove(uuid) ?: return
        noticesManager.dismissNotice(notice.id)
        unseenFriendRequestCount.set(unseenFriendRequests.size)
        unseenFriendRequestStates[uuid]?.set { false }
    }

    fun unseenFriendRequestCount(): WeakState<Int> {
        return WeakState(unseenFriendRequestCount)
    }

    override fun onConnect() {
    }

    override fun resetState() {
        unseenFriendRequests.clear()
        unseenFriendRequestStates.values.forEach { it.set(false) }
        unseenFriendRequestCount.set(0)
    }
}