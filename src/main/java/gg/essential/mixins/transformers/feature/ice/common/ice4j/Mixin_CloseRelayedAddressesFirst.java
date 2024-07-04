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
import org.ice4j.TransportAddress;
import org.ice4j.stack.StunStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Shutting down the TURN candidates involves sending a delete request to the TURN server, this requires that the local
 * socket for that turn server is still available, so we must close TURN candidates before closing any other sockets.
 *
 * @see Mixin_RememberRelayedAddresses
 */
@Mixin(value = StunStack.class, remap = false)
public abstract class Mixin_CloseRelayedAddressesFirst implements StunStackExt {
    @Unique
    private final Queue<TransportAddress> relayedAddresses = new ConcurrentLinkedQueue<>();

    @Override
    public void rememberToCloseFirst(TransportAddress address) {
        relayedAddresses.add(address);
    }

    @Inject(method = "shutDown", at = @At("HEAD"))
    private void closeRelayedCandidatesFirst(CallbackInfo ci) {
        TransportAddress address;
        while ((address = relayedAddresses.poll()) != null) {
            removeSocket(address);
        }
    }

    @Shadow
    public abstract void removeSocket(TransportAddress localAddr);
}
