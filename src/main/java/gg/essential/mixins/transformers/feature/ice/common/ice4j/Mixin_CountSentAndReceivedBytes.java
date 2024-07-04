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
package gg.essential.mixins.transformers.feature.ice.common.ice4j;

import gg.essential.mixins.impl.feature.ice.common.MergingDatagramSocketExt;
import gg.essential.network.connectionmanager.sps.SPSConnectionTelemetry;
import gg.essential.util.ProtocolUtils;
import org.ice4j.socket.MergingDatagramSocket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.DatagramPacket;

@Mixin(value = MergingDatagramSocket.class, remap = false)
public class Mixin_CountSentAndReceivedBytes implements MergingDatagramSocketExt {
    @Unique
    private SPSConnectionTelemetry essential$connectionTelemetry;

    @Override
    public void essential$setConnectionTelemetry(SPSConnectionTelemetry connectionTelemetry) {
        this.essential$connectionTelemetry = connectionTelemetry;
    }

    @ModifyVariable(
        method = "receive",
        at = @At(
            value = "RETURN",
            ordinal = 0 // There's a second return for an error condition, we don't want to track bytes from that
        ),
        argsOnly = true
    )
    private DatagramPacket essential$countReceivedBytes(DatagramPacket packet) {
        if (this.essential$connectionTelemetry != null) {
            this.essential$connectionTelemetry.packetReceived(ProtocolUtils.guessHeaderSize(packet) + packet.getLength());
        }
        return packet;
    }

    @Inject(method = "send", at = @At("HEAD"))
    private void essential$countSentBytes(DatagramPacket packet, CallbackInfo ci) {
        if (this.essential$connectionTelemetry != null) {
            this.essential$connectionTelemetry.packetSent(ProtocolUtils.guessHeaderSize(packet) + packet.getLength());
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void essential$onClose(CallbackInfo ci) {
        if (this.essential$connectionTelemetry != null) {
            this.essential$connectionTelemetry.send();
        }
    }
}
