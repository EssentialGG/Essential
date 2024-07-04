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
// 1.12.2 and below
package gg.essential.mixins.transformers.client.renderer;

import gg.essential.mixins.ext.client.renderer.PlayerSkinTextureExt;
import gg.essential.universal.UImage;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;

@Mixin(ThreadDownloadImageData.class)
public class MixinPlayerSkinTexture implements PlayerSkinTextureExt {

    @Shadow @Nullable private BufferedImage bufferedImage;

    @Nullable
    @Override
    public UImage essential$getImage() {
        return this.bufferedImage != null ? new UImage(this.bufferedImage) : null;
    }

    @Override
    public void essential$setImage(@Nullable UImage image) {
        this.bufferedImage = image != null ? image.getNativeImage() : null;
    }
}
