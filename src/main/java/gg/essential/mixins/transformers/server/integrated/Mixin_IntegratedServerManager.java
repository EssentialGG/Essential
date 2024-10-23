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
package gg.essential.mixins.transformers.server.integrated;

import gg.essential.mixins.ext.server.integrated.IntegratedServerExt;
import gg.essential.sps.McIntegratedServerManager;
import net.minecraft.server.integrated.IntegratedServer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
public abstract class Mixin_IntegratedServerManager implements IntegratedServerExt {
    @Unique
    private McIntegratedServerManager manager;

    // Note: Needs to be initialized after `this.mc` is set.
    //#if MC>=11200
    @Inject(method = "<init>", at = @At("RETURN"))
    //#else
    //$$ // 1.8.9 in its Minecraft constructor for some reason creates an IntegratedServer without any folder.
    //$$ // It doesn't appear to use it for anything and it's gone it 1.12.2, so we'll just skip creating our manager
    //$$ // in that case by targeting the regular constructor only.
    //$$ @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V", at = @At("RETURN"))
    //#endif
    private void initIntegratedServerManager(CallbackInfo ci) {
        manager = new McIntegratedServerManager((IntegratedServer) (Object) this);
    }

    @NotNull
    @Override
    public McIntegratedServerManager getEssential$manager() {
        return manager;
    }
}
