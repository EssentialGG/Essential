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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.universal.UGraphics;
import gg.essential.util.UIdentifier;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static gg.essential.util.UIdentifierKt.toMC;

//#if MC>=11600
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import com.mojang.blaze3d.vertex.IVertexBuilder;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import net.minecraft.entity.LivingEntity;
//#else
import net.minecraft.entity.Entity;
//#endif

@Mixin(LayerElytra.class)
public abstract class Mixin_Emissive_Elytra {

    //#if MC>=11600
    //$$ private static final String RENDER_LAYER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#if MC>=12100
    //$$ private static final String RENDER_ELYTRA = "Lnet/minecraft/client/render/entity/model/ElytraEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V";
    //#else
    //$$ private static final String RENDER_ELYTRA = "Lnet/minecraft/client/renderer/entity/model/ElytraModel;render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V";
    //#endif
    //#else
    private static final String RENDER_LAYER = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V";
    private static final String RENDER_ELYTRA = "Lnet/minecraft/client/model/ModelElytra;render(Lnet/minecraft/entity/Entity;FFFFFF)V";
    //#endif

    @WrapOperation(method = RENDER_LAYER, at = @At(value = "INVOKE", target = RENDER_ELYTRA))
    private void renderWithEmissiveLayer(
        ModelElytra model,
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //$$ IVertexBuilder vertexConsumer,
        //$$ int light,
        //$$ int overlay,
        //#if MC<12100
        //$$ float red,
        //$$ float green,
        //$$ float blue,
        //$$ float alpha,
        //#endif
        //#else
        Entity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        float scale,
        //#endif
        Operation<Void> original
        //#if MC>=11400
        //$$ , @Local(argsOnly = true) IRenderTypeBuffer buffer
        //$$ , @Local(argsOnly = true) LivingEntity entity
        //#endif
    ) {
        // Regular elytra
        original.call(
            model,
            //#if MC>=11400
            //$$ matrixStack,
            //$$ vertexConsumer,
            //$$ light,
            //$$ overlay
            //#if MC<12100
            //$$ , red
            //$$ , green
            //$$ , blue
            //$$ , alpha
            //#endif
            //#else
            entity,
            limbSwing,
            limbSwingAmount,
            ageInTicks,
            netHeadYaw,
            headPitch,
            scale
            //#endif
        );

        // Emissive layer
        if (!(entity instanceof AbstractClientPlayerExt)) {
            return;
        }
        AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entity;
        UIdentifier emissiveTexture = playerExt.getEmissiveCapeTexture();
        if (emissiveTexture == null) {
            return;
        }

        //#if MC>=11400
        //$$ original.call(
        //$$     model,
        //$$     matrixStack,
        //$$     buffer.getBuffer(MinecraftRenderBackend.INSTANCE.getEmissiveArmorLayer(toMC(emissiveTexture))),
        //$$     light,
        //$$     overlay
            //#if MC<12100
            //$$ , red
            //$$ , green
            //$$ , blue
            //$$ , alpha
            //#endif
        //$$ );
        //#else
        Function0<Unit> cleanup = MinecraftRenderBackend.INSTANCE.setupEmissiveRendering();
        UGraphics.bindTexture(0, toMC(emissiveTexture));
        original.call(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        cleanup.invoke();
        //#endif
    }
}
