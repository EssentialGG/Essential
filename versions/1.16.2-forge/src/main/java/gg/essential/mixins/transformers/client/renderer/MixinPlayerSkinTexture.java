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
// 1.16 and above
package gg.essential.mixins.transformers.client.renderer;

import gg.essential.mixins.ext.client.renderer.PlayerSkinTextureExt;
import gg.essential.universal.UImage;
import net.minecraft.client.renderer.texture.DownloadingTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DownloadingTexture.class)
public abstract class MixinPlayerSkinTexture extends SimpleTexture implements PlayerSkinTextureExt {

    @Shadow
    protected abstract void setImage(NativeImage nativeImageIn);

    @Unique
    private NativeImage image;

    public MixinPlayerSkinTexture(ResourceLocation textureResourceLocation) {
        super(textureResourceLocation);
    }

    @Nullable
    @Override
    public UImage essential$getImage() {
        return this.image == null ? null : new UImage(this.image);
    }

    @Override
    public void essential$setImage(@Nullable UImage image) {
        if (image != null) {
            this.setImage(image.getNativeImage());
        }
    }

    @Inject(method = "setImage", at = @At("HEAD"))
    private void storeImage(NativeImage image, CallbackInfo ci) {
        if (this.image != null) {
            this.image.close();
        }
        // Need to copy it cause MC will close it after the upload
        this.image = new NativeImage(image.getFormat(), image.getWidth(), image.getHeight(), false);
        this.image.copyImageData(image);
    }

    @Override
    public void close() {
        if (image != null) {
            image.close();
            image = null;
        }
        super.close();
    }
}
