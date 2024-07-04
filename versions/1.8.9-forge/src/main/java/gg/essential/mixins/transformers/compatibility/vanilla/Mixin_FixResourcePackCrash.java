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
package gg.essential.mixins.transformers.compatibility.vanilla;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.resources.ResourcePackRepository;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * We must create the "server-resource-packs" directory before accepting a resource pack from a server / SPS session.
 * If it doesn't exist, the game will crash.
 * <p></p>
 * This is resolved in 1.12.2, and most launchers have a workaround in place for this issue (creating the directory),
 * but some may not have this fix. This also fixes the game crashing in the development environment when using the Share
 * RP feature.
 * <p></p>
 * Linear: EM-1998
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@Mixin(ResourcePackRepository.class)
public class Mixin_FixResourcePackCrash {
    @Shadow
    @Final
    private File dirServerResourcepacks;

    @Inject(method = "downloadResourcePack", at = @At("HEAD"))
    private void essential$ensureServerResourcePacksDirectoryExists(CallbackInfoReturnable<ListenableFuture<?>> cir) {
        if (!this.dirServerResourcepacks.exists()) {
            this.dirServerResourcepacks.mkdirs();
        }
    }
}
