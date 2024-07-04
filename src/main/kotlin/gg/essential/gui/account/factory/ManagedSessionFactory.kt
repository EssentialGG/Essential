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
package gg.essential.gui.account.factory

import com.mojang.authlib.exceptions.AuthenticationException
import gg.essential.util.USession
import java.util.*

/** A session factory which is in full control over the sessions it can produce. */
interface ManagedSessionFactory : SessionFactory {
    /**
     * Tries to refresh the token of the given session. The session must belong this factory.
     *
     * Implementations must be thread-safe because this method is blocking and should as such
     * not be called from the main thread.
     */
    @Throws(AuthenticationException::class)
    fun refresh(session: USession, force: Boolean): USession

    /**
     * Removes the session associated with the given UUID from this factory. Does nothing if no such session exists.
     */
    fun remove(uuid: UUID)
}