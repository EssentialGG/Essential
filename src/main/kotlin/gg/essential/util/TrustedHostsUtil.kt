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

import gg.essential.universal.wrappers.UPlayer
import gg.essential.profiles.model.TrustedHost
import gg.essential.Essential
import gg.essential.api.utils.TrustedHostsUtil
import gg.essential.connectionmanager.common.packet.profile.trustedhosts.ClientProfileTrustedHostsCreatePacket
import gg.essential.connectionmanager.common.packet.profile.trustedhosts.ClientProfileTrustedHostsDeletePacket

object TrustedHostsUtil : TrustedHostsUtil {
    override fun getTrustedHosts(): Set<TrustedHostsUtil.TrustedHost> {
        val hosts = Essential.getInstance().connectionManager.profileManager.trustedHosts.values
        val a = mutableSetOf<TrustedHostsUtil.TrustedHost>()
        hosts.forEach {
            a.add(it.toApiHost())
        }
        return a
    }

    override fun getTrustedHostByID(id: String): TrustedHostsUtil.TrustedHost? =
        Essential.getInstance().connectionManager.profileManager.trustedHosts[id]?.toApiHost()

    override fun addTrustedHost(host: TrustedHostsUtil.TrustedHost) {
        val cm = Essential.getInstance().connectionManager
        cm.profileManager.addTrustedHost(host.toModel())
        cm.send(ClientProfileTrustedHostsCreatePacket(host.name, host.domains))
    }

    override fun removeTrustedHost(hostId: String) {
        val cm = Essential.getInstance().connectionManager
        cm.profileManager.removeTrustedHost(hostId)
        cm.send(ClientProfileTrustedHostsDeletePacket(hostId))
    }

    private fun TrustedHost.toApiHost(): TrustedHostsUtil.TrustedHost = TrustedHostsUtil.TrustedHost(id, name, domains)
    private fun TrustedHostsUtil.TrustedHost.toModel(): TrustedHost = TrustedHost(id, name, domains, UPlayer.getUUID())
}