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
package gg.essential.mixins.transformers.compatibility.forge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes (yet another) bug in Forge which breaks LAN worlds (and by extension SPS) if there are mods installed which
 * broadcast a custom packet to multiple players via Forge's networking API.
 *
 * This is caused by Forge using the internal ByteBuf of the packet directly, and reading from it in the client
 * packet handler code without restoring the reader index after they're done.
 * As a result, when the same ByteBuf is then to be sent to other players, it'll already have been read and effectively
 * appear empty, causing log spams and disconnects on the other side.
 *
 * This Mixin fixes that issue by returning a slice instead of the true internal ByteBuf from Forge's getInternalData
 * method.
 * Afaict Forge doesn't actually need the internal ByteBuf anywhere anyway.
 */
@Mixin(CustomPayloadS2CPacket.class)
public class Mixin_FixInternalByteBufAccess {
    @Dynamic("getInternalData is added by Forge PR #8973") // older versions also access the internal buffer, but patching those is probably not worth the effort
    @ModifyReturnValue(method = "getInternalData", at = @At("RETURN"), remap = false, expect = 0, require = 0)
    private PacketByteBuf slice(PacketByteBuf buf) {
        return buf != null ? new PacketByteBuf(buf.slice()) : null;
    }
}
