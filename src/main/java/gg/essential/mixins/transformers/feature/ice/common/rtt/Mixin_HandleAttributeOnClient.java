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

import org.ice4j.StunMessageEvent;
import org.ice4j.attribute.TransactionTransmitCounterAttribute;
import org.ice4j.message.Request;
import org.ice4j.message.Response;
import org.ice4j.stack.StunClientTransaction;
import org.ice4j.stack.TransactionID;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Mixin(value = StunClientTransaction.class, remap = false)
public abstract class Mixin_HandleAttributeOnClient {

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    @Final
    private Request request;

    @Shadow
    @Final
    private TransactionID transactionID;

    @Unique
    private final List<Instant> sendTime = new ArrayList<>();

    @Inject(method = "sendRequest0", at = @At("HEAD"))
    private void updateTTCAttribute(CallbackInfo ci) {
        // Get the current TTCA from the request
        TransactionTransmitCounterAttribute attribute = TransactionTransmitCounterAttribute.get(this.request);
        if (attribute == null) {
            // add it if it's not yet present
            this.request.putAttribute(attribute = new TransactionTransmitCounterAttribute());
        }

        // Store the time at which this request was sent
        this.sendTime.add(Instant.now());
        // and increment the counter in the request accordingly
        attribute.req = this.sendTime.size();
    }

    @Inject(method = "handleResponse", at = @At("HEAD"))
    private void computeRTT(StunMessageEvent evt, CallbackInfo ci) {
        Response response = (Response) evt.getMessage();
        TransactionTransmitCounterAttribute attribute = TransactionTransmitCounterAttribute.get(response);
        if (attribute == null) {
            return;
        }

        int index = attribute.req - 1;
        if (index < 0 || index >= sendTime.size()) {
            logger.warning("Received out of bounds " + attribute.getName() + " for " + this.transactionID
                + " (req: " + attribute.req + ", resp: " + attribute.resp + ", sent: " + sendTime.size() + ")");
            return;
        }
        attribute.rtt = Duration.between(sendTime.get(index), Instant.now());
    }
}
