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

import gg.essential.CompatAccessTransformer;
import gg.essential.CompatMixin;
import gg.essential.CompatShadow;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;


@CompatAccessTransformer(add = {Opcodes.ACC_PUBLIC})
@CompatMixin(target = "org.spongepowered.asm.mixin.transformer.MixinProcessor")
public class MixinProcessorCompat {
    @CompatShadow
    private Extensions extensions;

    @CompatShadow
    private int prepareConfigs(MixinEnvironment environment, Extensions extensions) {
        throw new LinkageError();
    }

    // Used via reflection by quite a few mods on 1.12.2, e.g. VanillaFix
    private int prepareConfigs(MixinEnvironment environment) {
        return prepareConfigs(environment, this.extensions);
    }
}
