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
import gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gg.essential.handlers.ModCompatBypass.BYPASS_MOD_COMPAT_MARKER;

@Mixin(value = FMLNetworkHandler.class, remap = false)
public abstract class Mixin_BypassModCompat_Check {
    @Inject(method = "checkModList(Lnet/minecraftforge/fml/common/network/handshake/FMLHandshakeMessage$ModList;Lnet/minecraftforge/fml/relauncher/Side;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private static void bypassModCompatCheck(FMLHandshakeMessage.ModList message, Side remoteSide, CallbackInfoReturnable<String> ci) {
        if (remoteSide == Side.SERVER) {
            ServerData currentServerData = Minecraft.getMinecraft().getCurrentServerData();
            if (currentServerData != null && ServerDataExtKt.getExt(currentServerData).getEssential$skipModCompatCheck()) {
                ci.setReturnValue(null);
            }
        } else {
            if (message.modList().containsKey(BYPASS_MOD_COMPAT_MARKER)) {
                ci.setReturnValue(null);
            }
        }
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_BypassModCompat_Check  {
//$$ }
//#endif
