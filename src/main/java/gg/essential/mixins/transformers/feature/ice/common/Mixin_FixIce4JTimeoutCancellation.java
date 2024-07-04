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
package gg.essential.mixins.transformers.feature.ice.common;

import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.IceMediaStream;
import org.jitsi.utils.logging2.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Ice4J schedules a timer once all known candidates have timed-out/completed. If no new candidates are found before that
 * timer expires, it'll mark the entire ICE session as failed.
 * Ice4J however neglected to actually cancel the timer when there were new successful candidates, so even when the
 * connection could have been established successfully, if that doesn't happen within the timer (which is by no means
 * guaranteed: e.g. if only turn candidates are found, we'll wait up to 10s to see if we can find any direct candidates
 * instead; this timer is only 5s), Ice4J will mark the entire session as failed anyway.
 *
 * This mixin fixes that issue by canceling the timer in this case. Specifically, it cancels the timer in such cases
 * where Ice4J would not have started a new one (we can't just inject the inverted check because those methods are
 * package-visible only, hence why we build an artificial else-branch).
 */
@Mixin(targets = "org/ice4j/ice/ConnectivityCheckClient", remap = false)
public abstract class Mixin_FixIce4JTimeoutCancellation {

    @Shadow
    private ConcurrentMap<String, ScheduledFuture<?>> checkListCompletionCheckers;

    @Shadow
    protected abstract void updateCheckListAndTimerStates(CandidatePair checkedPair);

    @Shadow
    private Logger logger;

    @Unique
    private final ThreadLocal<Boolean> shouldTimeOut = new ThreadLocal<>();

    @Inject(method = "updateCheckListAndTimerStates", at = @At("HEAD"))
    private void assumeShouldNotTimeOut(CallbackInfo ci) {
        shouldTimeOut.set(false);
    }

    @Inject(method = "updateCheckListAndTimerStates", at = @At(value = "FIELD", target = "Lorg/ice4j/ice/ConnectivityCheckClient;checkListCompletionCheckers:Ljava/util/concurrent/ConcurrentMap;", ordinal = 0))
    private void shouldActuallyTimeOut(CandidatePair checkedPair, CallbackInfo ci) {
        shouldTimeOut.set(true);
    }

    @Inject(method = "updateCheckListAndTimerStates", at = @At("HEAD"))
    private void cancelTimerIfItShouldNotTimeOut(CandidatePair checkedPair, CallbackInfo ci) {
        if (!shouldTimeOut.get()) {
            IceMediaStream stream = checkedPair.getParentComponent().getParentStream();
            ScheduledFuture<?> timer = checkListCompletionCheckers.remove(stream.getName());
            if (timer != null) {
                logger.info("Found new ongoing/succeeded checks, canceling CheckList timeout timer");

                timer.cancel(false);

                // If another thread found the final check to have been completed while we were in this method, and then
                // saw that a timeout was already present, it won't start a new one. But we just removed that timeout
                // so now we could be in the unlikely scenario where all pairs are completed but no timer is running.
                // To prevent that, we call the method again, which will then schedule a new timer if everything is
                // completed (or just not do anything noteworthy if nothing has changed).
                updateCheckListAndTimerStates(checkedPair);
            }
        }
    }
}
