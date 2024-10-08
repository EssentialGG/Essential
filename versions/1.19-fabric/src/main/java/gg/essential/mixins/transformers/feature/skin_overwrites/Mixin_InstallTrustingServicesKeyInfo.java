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
package gg.essential.mixins.transformers.feature.skin_overwrites;

// Applies only to 1.19+

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import gg.essential.handlers.GameProfileManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public class Mixin_InstallTrustingServicesKeyInfo {
    @ModifyReceiver(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/MinecraftSessionService;"))
    private YggdrasilAuthenticationService installTrustingServicesKeyInfo(YggdrasilAuthenticationService service) throws ReflectiveOperationException {
        GameProfileManager.register(service);
        return service;
    }
}
