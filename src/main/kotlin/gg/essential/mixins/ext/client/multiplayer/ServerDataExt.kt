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
package gg.essential.mixins.ext.client.multiplayer

import net.minecraft.client.multiplayer.ServerData

interface ServerDataExt {
    /**
     * Whether this server is sufficiently trusted by the user for us to connect to it directly.
     * This is set to false for servers fetched from external sources (friends, discovery, etc.).
     * It is true by default so we do not unnecessarily proxy pings for ServerData created by third-party mods (which
     * we can assume have already gotten explicit or implicit consent for what they are doing).
     *
     * If this is not true, then we proxy any ping requests via Essential servers as to protect the user's IP.
     * This is of special importance for the Friends tab as other users have fairly direct control of servers visible
     * there.
     */
    var `essential$isTrusted`: Boolean

    /** If the ping was proxied, this indicates the region from which the proxied ping latency was measured. */
    var `essential$pingRegion`: String?

    /** A value that will be stored into the ServerData's ping field after ServerPing stores its calculated ping. */
    var `essential$pingOverride`: Long?

    /** Whether to skip the mod compatibility check when connecting. */
    var `essential$skipModCompatCheck`: Boolean

    /** Whether the server is shared with friends. Refer to [EssentialConfig.sendServerUpdates] if `null`. */
    var `essential$shareWithFriends`: Boolean?
}

val ServerData.ext get() = this as ServerDataExt

var ServerDataExt.isTrusted
    get() = `essential$isTrusted`
    set(value) { `essential$isTrusted` = value }

var ServerDataExt.pingRegion
    get() = `essential$pingRegion`
    set(value) { `essential$pingRegion` = value }

var ServerDataExt.pingOverride
    get() = `essential$pingOverride`
    set(value) { `essential$pingOverride` = value }

var ServerDataExt.shareWithFriends
    get() = `essential$shareWithFriends`
    set(value) { `essential$shareWithFriends` = value }

