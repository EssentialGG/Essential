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
package gg.essential.mixins.transformers.feature.ice.common.rtt;

import gg.essential.mixins.impl.feature.ice.common.rtt.StunServerTransactionExt;
import org.ice4j.attribute.TransactionTransmitCounterAttribute;
import org.ice4j.message.Request;
import org.ice4j.message.Response;
import org.ice4j.stack.StunServerTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StunServerTransaction.class, remap = false)
public abstract class Mixin_HandleAttributeOnServer implements StunServerTransactionExt {

    @Shadow
    private Response response;

    @Unique
    private int repliesSent;

    @Inject(method = "retransmitResponse", at = @At(value = "INVOKE", target = "Lorg/ice4j/stack/NetAccessManager;sendMessage(Lorg/ice4j/message/Message;Lorg/ice4j/TransportAddress;Lorg/ice4j/TransportAddress;)V"))
    private void updateTTCAttribute(CallbackInfo ci) {
        Request request = this.getRequest();
        if (request == null) {
            return;
        }

        // Get the TTCA from the request
        TransactionTransmitCounterAttribute requestAttr = TransactionTransmitCounterAttribute.get(request);
        if (requestAttr == null) {
            return;
        }

        // Create the TTCA for the response
        TransactionTransmitCounterAttribute responseAttr = new TransactionTransmitCounterAttribute();
        responseAttr.req = requestAttr.req;
        responseAttr.resp = ++repliesSent;
        this.response.putAttribute(responseAttr);
    }
}
