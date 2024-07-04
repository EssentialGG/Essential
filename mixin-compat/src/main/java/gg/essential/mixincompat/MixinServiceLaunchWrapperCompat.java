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
package gg.essential.mixincompat;

import gg.essential.CompatMixin;
import gg.essential.CompatShadow;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;

@CompatMixin(MixinServiceLaunchWrapper.class)
public abstract class MixinServiceLaunchWrapperCompat {
    @CompatShadow(original = "prepare")
    public abstract void prepare$org();

    public void prepare() {
        prepare$org();

        // Initialize our 0.7 asm compat transformer (see BundledAsmTransformer class)
        Launch.classLoader.addTransformerExclusion("gg.essential.lib.guava21.");
        Launch.classLoader.registerTransformer(BundledAsmTransformer.class.getName());
    }
}
