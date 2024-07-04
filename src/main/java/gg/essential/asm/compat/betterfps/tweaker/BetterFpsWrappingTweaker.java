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
package gg.essential.asm.compat.betterfps.tweaker;

import com.google.common.collect.ImmutableSet;
import gg.essential.asm.compat.betterfps.BetterFpsTransformerWrapper;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class BetterFpsWrappingTweaker implements ITweaker {
    private static final Set<String> BROKEN_TRANSFORMERS = ImmutableSet.of("me.guichaguri.betterfps.transformers.EventTransformer", "me.guichaguri.betterfps.transformers.MathTransformer");

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        try {
            Field transformersField = LaunchClassLoader.class.getDeclaredField("transformers");
            transformersField.setAccessible(true);
            List<IClassTransformer> transformers = (List<IClassTransformer>) transformersField.get(classLoader);

            for (int i = 0; i < transformers.size(); i++) {
                IClassTransformer transformer = transformers.get(i);
                if (BROKEN_TRANSFORMERS.contains(transformer.getClass().getName())) {
                    transformers.set(i, new BetterFpsTransformerWrapper(transformer));
                }
            }
        } catch (Throwable e) {
            System.err.println("Failed to wrap BetterFPS' broken transformers! Chaos incoming...");
            e.printStackTrace();
        }
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
//#endif