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
package gg.essential.ice.stun

enum class StunType(val id: Int, private val requestResponse: Boolean, private val indication: Boolean) {
    Binding(0x001, true, true),
    Allocate(0x003, true, false),
    Refresh(0x004, true, false),
    Send(0x006, false, true),
    Data(0x007, false, true),
    CreatePermission(0x008, true, false),
    ChannelBind(0x009, true, false),
    ;

    fun isCompatible(cls: StunClass) = when (cls) {
        StunClass.Request, StunClass.ResponseSuccess, StunClass.ResponseError -> requestResponse
        StunClass.Indication -> indication
    }

    companion object {
        val byId = StunType.entries.associateBy { it.id }
    }
}
