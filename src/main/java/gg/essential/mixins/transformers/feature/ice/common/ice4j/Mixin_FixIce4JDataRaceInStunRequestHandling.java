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

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import org.ice4j.StunMessageEvent;
import org.ice4j.stack.StunServerTransaction;
import org.ice4j.stack.StunStack;
import org.ice4j.stack.TransactionID;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * There's a trivial data race in Ice4J's StunStack when checking for an existing transaction:
 * The check for an existing transaction is performed in a separate synchronized-block from the put of the new
 * transaction. As such, multiple threads can pass the initial check before the first one puts a transaction object.
 * The request will then be handled multiple times, with all but the first one throwing an exception when trying to send
 * the response.
 *
 * This mixin fixes the race by checking and putting in one block.
 * Due to what looks like a mixin bug, we cannot return (cancel) from within a synchronized block, so we open our own
 * shortly before the existing one (effectively making that one a no-op).
 */
@Mixin(value = StunStack.class, remap = false)
public abstract class Mixin_FixIce4JDataRaceInStunRequestHandling {
    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    @Final
    private Hashtable<TransactionID, StunServerTransaction> serverTransactions;

    @Unique
    private static final ThreadLocal<StunServerTransaction> newTransaction = new ThreadLocal<>();

    // TODO replace with local capture sugar once that becomes available in MixinExtras
    @ModifyReceiver(method = "handleMessageEvent", at = @At(value = "INVOKE", target = "Lorg/ice4j/stack/StunServerTransaction;start()V"))
    private StunServerTransaction captureNewTransaction(StunServerTransaction transaction) {
        newTransaction.set(transaction);
        return transaction;
    }

    @Inject(method = "handleMessageEvent", at = @At(value = "INVOKE", target = "Lorg/ice4j/stack/StunServerTransaction;start()V", shift = At.Shift.AFTER), cancellable = true)
    private void checkForRace(StunMessageEvent ev, CallbackInfo ci) {
        StunServerTransaction transaction = newTransaction.get();
        newTransaction.set(null);

        synchronized (this.serverTransactions) {
            StunServerTransaction existingTransaction = this.serverTransactions.get(ev.getTransactionID());
            if (existingTransaction == null) {
                // All good, we're the first (or only) one, so we can continue handling the request
                this.serverTransactions.put(ev.getTransactionID(), transaction);
                return;
            }

            // Another thread won the data race. Give up on our transaction, retransmit theirs instead.
            logger.finest("never mind that, found an existing transaction after all");
            try {
                // method's protected, can't compile a direct invocation (but the applied mixin does have access)
                StunServerTransaction.class.getDeclaredMethod("retransmitResponse").invoke(existingTransaction);
                logger.finest("Response retransmitted");
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to retransmit a stun response", ex);
            }
            ci.cancel();
        }
    }
}
