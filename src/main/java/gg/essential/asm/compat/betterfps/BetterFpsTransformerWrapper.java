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
//#if MC<=11202
package gg.essential.asm.compat.betterfps;

import gg.essential.asm.compat.betterfps.tweaker.BetterFpsWrappingTweaker;
import gg.essential.util.MixinUtils;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import java.util.List;

// Wraps transformers from BetterFPS which replace a null class with an empty one, breaking lots of stuff.
public class BetterFpsTransformerWrapper implements IClassTransformer {
    private final IClassTransformer delegate;

    public BetterFpsTransformerWrapper(IClassTransformer delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        return delegate.transform(name, transformedName, basicClass);
    }

    @SuppressWarnings("unchecked")
    public static void initialize() {
        MixinUtils.addTransformerExclusion(BetterFpsTransformerWrapper.class.getName());
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
        tweakClasses.add(BetterFpsWrappingTweaker.class.getName());
    }
}
//#endif