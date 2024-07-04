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
package gg.essential.util;

import gg.essential.render.TextRenderTypeVertexConsumer;
import gg.essential.universal.UGraphics;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.shader.BlendState;

//#if MC>=11600
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//#endif

public class Diamond {
    public static void drawDiamond(UMatrixStack matrixStack, int faceSize, float xCenter, float yCenter, int color) {
        BlendState prevBlendState = BlendState.active();
        BlendState.NORMAL.activate();

        //#if MC>=11600
        //$$ IRenderTypeBuffer.Impl provider = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        //$$ TextRenderTypeVertexConsumer vertexConsumer = TextRenderTypeVertexConsumer.create(provider);
        //#else
        UGraphics buffer = UGraphics.getFromTessellator();
        TextRenderTypeVertexConsumer vertexConsumer = TextRenderTypeVertexConsumer.create(buffer);
        //#endif

        drawDiamond(matrixStack, vertexConsumer, faceSize, xCenter, yCenter, color);

        //#if MC>=11600
        //$$ provider.finish();
        //#else
        buffer.drawDirect();
        //#endif

        prevBlendState.activate();
    }

    public static void drawDiamond(UMatrixStack matrixStack, TextRenderTypeVertexConsumer vertexConsumer, int faceSize, float xCenter, float yCenter, int color) {
        // light: LightmapTextureManager#MAX_LIGHT_COORDINATE
        drawDiamond(matrixStack, vertexConsumer, faceSize, xCenter, yCenter, color, 0xF000F0);
    }

    public static void drawDiamond(UMatrixStack matrixStack, TextRenderTypeVertexConsumer consumer, int faceSize, float xCenter, float yCenter, int color, int light) {
        int alpha = (color >> 24) & 255;
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;

        double radius = faceSize / 1.75; // Arbitrary scale factor. Renders at about the same size as the pixelated one.
        consumer.pos(matrixStack, xCenter, yCenter - radius, 0.0).color(red, green, blue, alpha).tex(0, 0).light(light).endVertex();
        consumer.pos(matrixStack, xCenter - radius, yCenter, 0.0).color(red, green, blue, alpha).tex(0, 0).light(light).endVertex();
        consumer.pos(matrixStack, xCenter, yCenter + radius, 0.0).color(red, green, blue, alpha).tex(0, 0).light(light).endVertex();
        consumer.pos(matrixStack, xCenter + radius, yCenter, 0.0).color(red, green, blue, alpha).tex(0, 0).light(light).endVertex();
    }
}
