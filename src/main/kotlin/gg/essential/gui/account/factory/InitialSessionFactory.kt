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

import gg.essential.universal.UMinecraft
import gg.essential.util.USession
import gg.essential.util.toUSession
import java.util.*

class InitialSessionFactory: SessionFactory {
    override val sessions: Map<UUID, USession> by lazy {
        mapOf(UMinecraft.getMinecraft().session.toUSession().let { it.uuid to it })
    }
}