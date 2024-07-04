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
package gg.essential.util

import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import java.util.UUID

data class USession(val uuid: UUID, val username: String, val token: String) {
    companion object {
        private val state = mutableStateOf(USession(UUID(0L, 0L), "", ""))
        init {
            platform.registerActiveSessionState(state)
        }

        val active: State<USession> = state
        fun activeNow(): USession = state.get()
    }
}
