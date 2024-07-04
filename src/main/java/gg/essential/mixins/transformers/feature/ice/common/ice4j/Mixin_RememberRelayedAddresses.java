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

import gg.essential.mixins.impl.feature.ice.common.StunStackExt;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.harvest.StunCandidateHarvest;
import org.ice4j.ice.harvest.TurnCandidateHarvest;
import org.ice4j.socket.IceSocketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * @see Mixin_CloseRelayedAddressesFirst
 */
@Mixin(value = TurnCandidateHarvest.class, remap = false)
public abstract class Mixin_RememberRelayedAddresses extends StunCandidateHarvest {
    @ModifyArg(method = "createRelayedCandidate(Lorg/ice4j/message/Response;)V", at = @At(value = "INVOKE", target = "Lorg/ice4j/stack/StunStack;addSocket(Lorg/ice4j/socket/IceSocketWrapper;)V"))
    private IceSocketWrapper rememberAddress(IceSocketWrapper socket) {
        Transport transport = socket.getUDPSocket() != null ? Transport.UDP : Transport.TCP;
        TransportAddress address = new TransportAddress(socket.getLocalAddress(), socket.getLocalPort(), transport);
        ((StunStackExt) harvester.getStunStack()).rememberToCloseFirst(address);
        return socket;
    }

    public Mixin_RememberRelayedAddresses() {
        super(null, null);
    }
}
