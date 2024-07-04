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
package gg.essential.mixins.transformers.resources;

import gg.essential.util.HelpersKt;
import net.minecraft.client.resources.IResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.List;

//#if MC>=11400
//$$ import net.minecraft.resources.ResourcePackList;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$ import java.util.ArrayList;
//#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

//#if MC>=11400
//$$ @Mixin(ResourcePackList.class)
//$$ public class MixinResourcePackList {
//$$     @Inject(method = "func_232623_f_", at = @At("RETURN"), cancellable = true)
//$$     private void addEssentialResourcePack(CallbackInfoReturnable<List<IResourcePack>> ci) {
//$$         // The returned list is immutable by default, so we need to copy it
//$$         List<IResourcePack> list = new ArrayList<>(ci.getReturnValue());
//$$         HelpersKt.addEssentialResourcePack(list::add);
//$$         ci.setReturnValue(list);
//$$     }
//$$ }
//#else
@Mixin(Minecraft.class)
public class MixinResourcePackList {

    @Shadow @Final private List<IResourcePack> defaultResourcePacks;

    @Inject(method = "init", at = @At("HEAD"))
    private void addEssentialResourcePack(CallbackInfo ci) {
        HelpersKt.addEssentialResourcePack(this.defaultResourcePacks::add);
    }
}
//#endif
