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
package gg.essential.mixins.transformers.client.renderer.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.matrix.MatrixStack;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.wrappers.message.UTextComponent;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class Mixin_RenderNameplateIcon<T extends Entity> {
    @Inject(method = "renderName", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;scale(FFF)V", shift = At.Shift.AFTER, ordinal = 0))
    private void essential$translateNameplate(CallbackInfo ci, @Local(argsOnly = true) T entity, @Local(argsOnly = true) MatrixStack matrixStack) {
    }

    @Inject(method = "renderName", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;pop()V"))
    private void renderEssentialIndicator(
        T entity,
        ITextComponent name,
        MatrixStack vMatrixStack,
        IRenderTypeBuffer bufferIn,
        int packedLightIn,
        //#if MC>=12005
        //$$ float timeDelta,
        //#endif
        CallbackInfo ci
    ) {
       if (OnlineIndicator.currentlyDrawingEntityName()) {
           OnlineIndicator.drawNametagIndicator(new UMatrixStack(vMatrixStack), bufferIn, entity, new UTextComponent(name.deepCopy()).getFormattedText(), packedLightIn);
       }
    }
}
