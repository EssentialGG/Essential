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
package gg.essential.mixins.transformers.forge;
//#if MC<=11202

import net.minecraftforge.fml.common.discovery.JarDiscoverer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = JarDiscoverer.class, remap = false)
public abstract class MixinJarDiscoverer {
    @Redirect(method = {"discover", "findClassesASM"}, at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z"))
    private boolean shouldSkip(String entry, String originalPattern) {
        // Do not try to look for mods in the Java 9 version specific directory (kotlin includes one).
        // Forge cannot read those and LaunchWrapper cannot load them, so there's no point trying and spamming
        // the log with warnings about them being "corrupt" (seems like forge never considered forwards-compatibility).
        if (entry.startsWith("META-INF/versions/9/")) {
            return true;
        }
        return entry.startsWith(originalPattern);
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class MixinJarDiscoverer {
//$$ }
//#endif
