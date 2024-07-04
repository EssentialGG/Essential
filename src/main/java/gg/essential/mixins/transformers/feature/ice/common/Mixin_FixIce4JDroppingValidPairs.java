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

import gg.essential.network.connectionmanager.ice.util.StrictComparator;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.IceMediaStream;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * Ice4J stores validated CandidatePairs in a validList (which is a TreeSet). TreeSet uses exclusively compareTo to
 * compare entries. CandidatePair implements that method by only comparing the priority but doesn't guarantee that each
 * candidate actually has a unique priority, so some (actually quite a lot) of pairs get lost because the TreeSet thinks
 * they're already in there.
 * <p>
 * Fixing the priority is quite complicated, so we'll simply replace the TreeSet with one which uses a comparator which
 * also checks equals (we can't just replace it with a different set because the field type is TreeSet as well).
 */
@Mixin(value = IceMediaStream.class, remap = false)
public abstract class Mixin_FixIce4JDroppingValidPairs {
    @Shadow
    @Final
    @Mutable
    private TreeSet<CandidatePair> validList;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceValidListWithOneWhichDoesNotDropSamePriorityPairs(CallbackInfo ci) {
        this.validList = new TreeSet<>(new StrictComparator<CandidatePair>(Comparator.naturalOrder()));
    }
}
