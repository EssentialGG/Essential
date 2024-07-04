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
package gg.essential.mixins.transformers.client.resources;

import gg.essential.mixins.impl.client.resources.PlayerSkinProviderAccessor;
import net.minecraft.client.texture.PlayerSkinProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(PlayerSkinProvider.class)
public class PlayerSkinProviderAccessorMixin implements PlayerSkinProviderAccessor {
    @Unique
    private SkinProviderFileCacheAccessor skinCache;
    @Unique
    private SkinProviderFileCacheAccessor capeCache;
    @Unique
    private SkinProviderFileCacheAccessor elytraCache;

    @Override
    public SkinProviderFileCacheAccessor getSkinCache() {
        return this.skinCache;
    }

    @Override
    public SkinProviderFileCacheAccessor getCapeCache() {
        return this.capeCache;
    }

    @Override
    public SkinProviderFileCacheAccessor getElytraCache() {
        return this.elytraCache;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) throws IllegalAccessException {
        for (Field field : PlayerSkinProvider.class.getDeclaredFields()) {
            if (!SkinProviderFileCacheAccessor.class.isAssignableFrom(field.getType())) {
                continue;
            }
            SkinProviderFileCacheAccessor fileCache = (SkinProviderFileCacheAccessor) field.get(this);
            switch (fileCache.getType()) {
                case SKIN -> this.skinCache = fileCache;
                case CAPE -> this.capeCache = fileCache;
                case ELYTRA -> this.elytraCache = fileCache;
            }
        }
    }
}
