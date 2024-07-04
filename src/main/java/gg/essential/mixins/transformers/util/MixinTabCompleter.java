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
package gg.essential.mixins.transformers.util;

import gg.essential.commands.EssentialCommandRegistry;
import com.google.common.collect.ObjectArrays;
//#if MC==11202
import net.minecraft.util.TabCompleter;
//#else
//$$ import gg.essential.mixins.DummyTarget;
//#endif

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC==11202
@Mixin(TabCompleter.class)
//#else
//$$ @Mixin(DummyTarget.class)
//#endif
public class MixinTabCompleter {
    //#if MC==11202
    private String[] essential$completionOptions = new String[0];

    @Inject(method = "requestCompletions", at = @At("HEAD"))
    public void onRequest(String request, CallbackInfo info) {
        if (request.length() >= 1)
            essential$completionOptions = EssentialCommandRegistry.INSTANCE.getCompletionOptions(request);
    }

    @ModifyVariable(method = "setCompletions", at = @At("HEAD"))
    public String[] sendChatMessage(String... message) {
        return ObjectArrays.concat(message, essential$completionOptions, String.class);
    }
    //#endif
}
