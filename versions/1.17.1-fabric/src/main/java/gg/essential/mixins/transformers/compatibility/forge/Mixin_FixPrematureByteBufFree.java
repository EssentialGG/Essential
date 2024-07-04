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

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a bug in Forge which breaks LAN worlds (and by extension SPS) if there are mods installed which broadcast a
 * custom packet to all players.
 * Introduced in https://github.com/MinecraftForge/MinecraftForge/pull/8042
 * Fixed in https://github.com/MinecraftForge/MinecraftForge/pull/8181
 * This is effectively a backport of the "fix" to affected 1.17 and 1.18 versions.
 */
@Mixin(CustomPayloadS2CPacket.class)
public class Mixin_FixPrematureByteBufFree {
    @Unique
    private boolean mayFreeByteBuf;

    @Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("TAIL"))
    private void takeByteBufOwnership(CallbackInfo ci) {
        this.mayFreeByteBuf = true;
    }

    @Dynamic("FriendlyByteBuf.release is added by Forge PR #8042")
    @Redirect(
        method = "apply(Lnet/minecraft/network/listener/ClientPlayPacketListener;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;release()Z"),
        require = 0,
        expect = 0)
    private boolean freeByteBufIfOwned(PacketByteBuf buf) {
        if (mayFreeByteBuf) {
            return buf.release();
        } else {
            return false;
        }
    }
}
