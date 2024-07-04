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
// 1.19.3+
// Older version load sounds directly from the resource packs, and are therefore served by EssentialAssetResourcePack.
// As of 1.19.3, MC enumerates all sound resources once (during the loading screen) and then uses a simple lookup map
// afterwards; since we download our assets dynamically, that's no good for us, so this mixin lets our assets bypass
// the lookup map.

package gg.essential.mixins.transformers.feature.sound;

import gg.essential.Essential;
import gg.essential.util.resource.EssentialAssetResourcePack;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;
import java.util.Optional;

@Mixin(SoundLoader.class)
public class Mixin_LoadFromEssentialAsset {
    @Unique
    private final EssentialAssetResourcePack essentialAssetResourcePack = new EssentialAssetResourcePack(Essential.getInstance().getConnectionManager().getCosmeticsManager().getAssetLoader());

    @ModifyVariable(method = "<init>", at = @At(
        // value = "ESSENTIAL:FIELD_IN_INIT",
        value = "LOAD", ordinal = 0
    ), argsOnly = true)
    private ResourceFactory withEssentialAssetSupport(ResourceFactory inner) {
        return id -> {
            Optional<Resource> resource = inner.getResource(id);
            if (resource.isPresent() || !"essential".equals(id.getNamespace())) {
                return resource;
            }
            InputSupplier<InputStream> inputSupplier = essentialAssetResourcePack.open(ResourceType.CLIENT_RESOURCES, id);
            if (inputSupplier == null) {
                return Optional.empty();
            }
            return Optional.of(new Resource(essentialAssetResourcePack, inputSupplier));
        };
    }
}
