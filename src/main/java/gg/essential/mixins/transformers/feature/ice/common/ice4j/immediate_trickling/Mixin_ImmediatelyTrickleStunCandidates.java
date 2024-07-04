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
package gg.essential.mixins.transformers.feature.ice.common.ice4j.immediate_trickling;

import gg.essential.mixins.impl.feature.ice.common.TricklingCandidateHarvester;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.harvest.StunCandidateHarvest;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TrickleCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(value = StunCandidateHarvester.class, remap = false)
public abstract class Mixin_ImmediatelyTrickleStunCandidates implements TricklingCandidateHarvester {
    @Unique
    private TrickleCallback trickleCallback;

    @Unique
    private final List<StunCandidateHarvest> alreadyTrickled = new ArrayList<>();

    @Shadow
    @Final
    private List<StunCandidateHarvest> completedHarvests;

    @Shadow
    @Final
    private List<StunCandidateHarvest> startedHarvests;

    @Override
    public void setTrickleCallback(TrickleCallback trickleCallback) {
        this.trickleCallback = trickleCallback;
    }

    @Inject(method = "waitForResolutionEnd", at = @At(value = "INVOKE", target = "Ljava/lang/Object;wait()V", shift = At.Shift.AFTER))
    private void trickleNewCandidates(CallbackInfo ci) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (trickleCallback == null) {
            return;
        }

        List<StunCandidateHarvest> newHarvests = new ArrayList<>();
        synchronized (this.completedHarvests) {
            for (StunCandidateHarvest completedHarvest : this.completedHarvests) {
                if (!alreadyTrickled.contains(completedHarvest)) {
                    alreadyTrickled.add(completedHarvest);
                    newHarvests.add(completedHarvest);
                }
            }
        }

        for (StunCandidateHarvest harvest : newHarvests) {
            // method's protected, can't compile a direct invocation (but the applied mixin does have access)
            Method getCandidates = StunCandidateHarvest.class.getDeclaredMethod("getCandidates");
            LocalCandidate[] candidates = (LocalCandidate[]) getCandidates.invoke(harvest);
            trickleCallback.onIceCandidates(Arrays.asList(candidates));
        }
    }

    @Inject(method = "completedResolvingCandidate", at = @At("RETURN"))
    private void alwaysWakeWaitingThread(CallbackInfo ci) {
        // So we get to trickle the new candidates asap. And the `wait` call is in a loop, so waking up is always safe.
        synchronized (this.startedHarvests) {
            this.startedHarvests.notify();
        }
    }
}
