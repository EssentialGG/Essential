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

//#if MC == 10809 && FORGE
//$$ import com.google.common.collect.Multimap;
//$$ import net.minecraftforge.fml.common.LoadController;
//$$ import net.minecraftforge.fml.common.Loader;
//$$ import net.minecraftforge.fml.common.LoaderState;
//$$ import gg.essential.util.HelpersKt;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif
import gg.essential.mixins.DummyTarget;
import org.spongepowered.asm.mixin.Mixin;

//#if MC == 10809 && FORGE
//$$ @Mixin(value = LoadController.class, remap = false)
//#else
@Mixin(DummyTarget.class)
//#endif
public class MixinLoadController {
    //#if MC == 10809
    //$$ @Shadow private Loader loader;
    //$$ @Shadow private Multimap<String, LoaderState.ModState> modStates;
    //$$
    //$$ @Inject(method = "printModStates", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/Loader;getModList()Ljava/util/List;", shift = At.Shift.BEFORE), cancellable = true)
    //$$ private void printModStates(StringBuilder ret, CallbackInfo ci) {
    //$$     ret.append("\n\n");
    //$$     ret.append(HelpersKt.makeModsTable(loader, modStates));
    //$$     ci.cancel();
    //$$ }
    //#endif
}
