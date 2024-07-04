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
package gg.essential.mixins.transformers.crash;

import net.minecraft.crash.CrashReport;
import gg.essential.util.crash.StacktraceDeobfuscator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public class MixinCrashReport {
    //#if MC>=11300
    //$$ // TODO
    //#else
    @Shadow @Final
    private Throwable cause;

    @Inject(method = "populateEnvironment", at = @At("HEAD"))
    public void populate(CallbackInfo info) {
        StacktraceDeobfuscator deobfuscator = StacktraceDeobfuscator.get();
        if (deobfuscator != null) {
            deobfuscator.deobfuscateThrowable(cause);
        }
    }
    //#endif
}
