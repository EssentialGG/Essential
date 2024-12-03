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
package gg.essential.cosmetics;

import gg.essential.model.EnumPart;
import gg.essential.model.ModelInstance;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.RenderBackend;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static gg.essential.cosmetics.EssentialModelRendererKt.flush;
import static gg.essential.cosmetics.EssentialModelRendererKt.renderForHoverOutline;
import static gg.essential.util.ExtensionsKt.toCommon;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//#endif

//#if MC>=11400
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.entity.model.PlayerModel;
//#else
import dev.folomeev.kotgl.matrix.vectors.Vec3;
import gg.essential.universal.UGraphics;
import static gg.essential.model.backend.minecraft.LegacyCameraPositioningKt.getRelativeCameraPosFromGlState;
//#endif

//#if MC>=12102
//$$ public class EssentialModelRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
//#elseif MC>=11400
//$$ public class EssentialModelRenderer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
//#else
public class EssentialModelRenderer implements LayerRenderer<AbstractClientPlayer> {
//#endif

    /**
     * Flag to skip cosmetic rendering
     */
    public static boolean suppressCosmeticRendering = false;
    private final RenderPlayer playerRenderer;

    public EssentialModelRenderer(RenderPlayer playerRenderer) {
        //#if MC>=11400
        //$$ super(playerRenderer);
        //#endif
        this.playerRenderer = playerRenderer;
    }

    public static boolean shouldRender(AbstractClientPlayer player) {
        if (suppressCosmeticRendering) {
            return false;
        }

        return !player.isInvisible() && !player.isSpectator();
    }

    public void render(
        UMatrixStack matrixStack,
        RenderBackend.VertexConsumerProvider vertexConsumerProvider,
        @NotNull CosmeticsRenderState cState,
        @Nullable Set<EnumPart> parts
    ) {
        WearablesManager wearablesManager = cState.wearablesManager();
        if (wearablesManager == null) {
            return;
        }
        Map<Cosmetic, ModelInstance> models = wearablesManager.getModels();
        if (models.isEmpty()) {
            return;
        }

        PlayerPose pose = PlayerPoseKt.toPose(playerRenderer);
        RenderBackend.Texture skin = new MinecraftRenderBackend.SkinTexture(cState.skinTexture());

        matrixStack.push();

        //#if MC<11400
        // Reposition our stack such that the camera is at 0/0/0, this is important for translucent geometry because
        // those are sorted relative to 0/0/0.
        // Modern versions have two separate stack and the passed one already fulfills this requirement, older versions
        // however don't, so we need to create this split artificially. Luckily our renderer already uses an explicit
        // matrix stack, so this is as simple as offsetting that in one direction and the global stack in the other to
        // balance it out.
        Vec3 relativeCamera = getRelativeCameraPosFromGlState();
        matrixStack.translate(-relativeCamera.getX(), -relativeCamera.getY(), -relativeCamera.getZ());
        UGraphics.GL.pushMatrix();
        UGraphics.GL.translate(relativeCamera.getX(), relativeCamera.getY(), relativeCamera.getZ());
        //#endif

        //#if MC<11400
        if (cState.isSneaking() && parts == null) {
            matrixStack.translate(0.0F, 0.2F, 0.0F); // from LayerCustomHead
        }
        //#endif

        if (parts == null) {
            parts = new HashSet<>(Arrays.asList(EnumPart.values()));
        }

        matrixStack.translate(0.0F, 1.501f, 0.0F); // undo RenderLivingBase.prepareScale

        //#if MC<11700
        GlStateManager.enableRescaleNormal();
        //#endif

        wearablesManager.render(toCommon(matrixStack), vertexConsumerProvider, pose, skin, parts);
        renderForHoverOutline(wearablesManager, toCommon(matrixStack), vertexConsumerProvider, pose, skin, parts);

        //#if MC<11700
        flush(vertexConsumerProvider);
        GlStateManager.disableRescaleNormal();
        //#endif

        matrixStack.pop();
        //#if MC<11400
        UGraphics.GL.popMatrix();
        //#endif

        cState.setRenderedPose(pose);
    }

    @Override
    //#if MC>=11400
    //#if MC>=12102
    //$$ public void render(MatrixStack vMatrixStack, VertexConsumerProvider buffer, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
    //$$     CosmeticsRenderState cState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
    //#else
    //$$ public void render(@NotNull MatrixStack vMatrixStack, @NotNull IRenderTypeBuffer buffer, int light, @NotNull AbstractClientPlayerEntity player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
    //$$     CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
    //#endif
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //$$     RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider(buffer, light);
    //#else
    public void doRenderLayer(@NotNull AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        UMatrixStack matrixStack = new UMatrixStack();
        RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider();
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
        //#endif
        render(matrixStack, vertexConsumerProvider, cState, null);
    }

    //#if MC < 11400
    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
    //#endif
}
