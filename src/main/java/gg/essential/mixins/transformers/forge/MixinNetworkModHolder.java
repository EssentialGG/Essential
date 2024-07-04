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
import gg.essential.util.ClientSideModUtil;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.network.internal.NetworkModHolder;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = NetworkModHolder.class, remap = false)
public class MixinNetworkModHolder {
    @Shadow private ModContainer container;

    @Inject(method = "check", at = @At("HEAD"), cancellable = true, remap = false)
    public void check(Map<String, String> data, Side side, CallbackInfoReturnable<Boolean> cir) {
        if (ClientSideModUtil.isModClientSide(container)) {
            cir.setReturnValue(true);
        }
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class MixinNetworkModHolder  {
//$$ }
//#endif
