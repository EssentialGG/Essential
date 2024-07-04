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
package gg.essential.mixins.transformers.compatibility.labymod;

//#if MC<=11202
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "net.labymod.gui.ModGuiMultiplayer")
public class MixinModGuiMultiplayer {
    @SuppressWarnings({"UnresolvedMixinReference", "DefaultAnnotationParam"})
    @ModifyArg(method = "func_73866_w_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ServerSelectionList;setDimensions(IIII)V", remap = true), index = 2, remap = false)
    private int shiftListDown(int topSpace) {
        return topSpace + 32;
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class MixinModGuiMultiplayer   {
//$$ }
//#endif
