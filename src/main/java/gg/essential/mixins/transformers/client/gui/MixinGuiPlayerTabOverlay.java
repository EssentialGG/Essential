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
package gg.essential.mixins.transformers.client.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import gg.essential.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.util.text.ITextComponent;
//#endif

//#if MC>=12000
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

// Decreased priority to avoid conflict with Feather Mixin due to https://github.com/SpongePowered/Mixin/issues/544
@Mixin(value = GuiPlayerTabOverlay.class, priority = 900)
public class MixinGuiPlayerTabOverlay {

    @ModifyExpressionValue(
        method = "renderPlayerlist",
        at = @At(
            value = "INVOKE",
            //#if MC>=11600
            //$$ target = "Lnet/minecraft/client/gui/FontRenderer;getStringPropertyWidth(Lnet/minecraft/util/text/ITextProperties;)I",
            //#else
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I",
            //#endif
            ordinal = 0
        )
    )
    private int essential$increaseWidthForIcon(int x, @Local NetworkPlayerInfo networkPlayerInfo) {
        return x;
    }

    //#if MC>=12000
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"), index = 2)
    //$$ private int essential$shiftNameTextAndRenderIcon(TextRenderer textRenderer, Text text, int x, int y, int color, @Local(argsOnly = true) DrawContext context, @Local PlayerListEntry networkPlayerInfo) {
    //$$     VertexConsumerProvider.Immediate provider = context.getVertexConsumers();
    //$$     UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
    //#else
    @ModifyArg(
        method = "renderPlayerlist",
        //#if MC>=11600
        //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;func_243246_a(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/util/text/ITextComponent;FFI)I"),
        //$$ index = 2
        //#else
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawScaledCustomSizeModalRect(IIFFIIIIFF)V", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V")
        ),
        index = 1
        //#endif
    )
    //#if MC>=11600
    //$$ private float essential$shiftNameTextAndRenderIcon(MatrixStack stack, ITextComponent text, float x, float y, int color, @Local NetworkPlayerInfo networkPlayerInfo) {
    //$$     IRenderTypeBuffer.Impl provider = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
    //$$     UMatrixStack matrixStack = new UMatrixStack(stack);
    //#else
    private float essential$shiftNameTextAndRenderIcon(String text, float x, float y, int color, @Local NetworkPlayerInfo networkPlayerInfo) {
        UMatrixStack matrixStack = new UMatrixStack();
    //#endif
    //#endif

        OnlineIndicator.drawTabIndicatorOuter(
            matrixStack,
            //#if MC>=11600
            //$$ provider,
            //#endif
            networkPlayerInfo,
            (int) x, (int) y
        );

        return x;
    }
}
