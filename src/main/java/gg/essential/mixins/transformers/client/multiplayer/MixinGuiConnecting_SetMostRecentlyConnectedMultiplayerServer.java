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
package gg.essential.mixins.transformers.client.multiplayer;

import gg.essential.universal.UMinecraft;
import gg.essential.util.ServerConnectionUtil;
import gg.essential.util.ServerDataInfo;
import net.minecraft.client.multiplayer.GuiConnecting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11903
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import net.minecraft.client.network.ServerInfo;
//#endif

//#if MC>=11701
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.network.ServerAddress;
//#endif

@Mixin(GuiConnecting.class)
public class MixinGuiConnecting_SetMostRecentlyConnectedMultiplayerServer {

    //#if MC<11701
    @Inject(method = "connect(Ljava/lang/String;I)V", at = @At("HEAD"))
    private void onGuiConnecting(String hostName, int port, CallbackInfo ci) {
        ServerConnectionUtil.setMostRecentServerInfo(new ServerDataInfo(hostName, port, UMinecraft.getMinecraft().getCurrentServerData()));
        ServerConnectionUtil.setHasRefreshed(false);
    }
    //#elseif MC<11903
    //$$ @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;)V", at = @At("HEAD"))
    //$$ private void onGuiConnecting(MinecraftClient minecraft, ServerAddress serverAddress, CallbackInfo ci) {
    //$$     ServerConnectionUtil.setMostRecentServerInfo(new ServerDataInfo(serverAddress.getAddress(), serverAddress.getPort(), UMinecraft.getMinecraft().getCurrentServerEntry()));
    //$$     ServerConnectionUtil.setHasRefreshed(false);
    //$$ }
    //#else
    //$$ @Inject(
        //#if MC>=12005
        //$$ method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V",
        //#else
        //$$ method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;)V",
        //#endif
    //$$     at = @At("HEAD")
    //$$ )
    //$$ private void onGuiConnecting(CallbackInfo ci, @Local(argsOnly = true) ServerAddress serverAddress, @Local(argsOnly = true) ServerInfo info) {
    //$$     ServerConnectionUtil.setMostRecentServerInfo(new ServerDataInfo(serverAddress.getAddress(), serverAddress.getPort(), info));
    //$$     ServerConnectionUtil.setHasRefreshed(false);
    //$$ }
    //#endif
}
