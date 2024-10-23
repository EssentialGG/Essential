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

import gg.essential.config.EssentialConfig;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.handlers.EssentialSoundManager;
import gg.essential.mixins.ext.client.ParticleSystemHolder;
import gg.essential.model.EnumPart;
import gg.essential.model.ModelAnimationState;
import gg.essential.model.ModelInstance;
import gg.essential.model.ParticleSystem;
import gg.essential.model.PlayerMolangQuery;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.RenderBackend;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import kotlin.Unit;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import gg.essential.data.OnboardingData;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static gg.essential.cosmetics.EssentialModelRendererKt.flush;
import static gg.essential.cosmetics.EssentialModelRendererKt.renderForHoverOutline;
import static gg.essential.gui.elementa.state.v2.StateKt.stateOf;
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

    // this is modified in Patcher to take Entity Render Distance into consideration, don't remove or rename
    public static boolean cosmeticsShouldRender(AbstractClientPlayer player) {
        if (!EssentialConfig.INSTANCE.getEssentialEnabled()) {
            return false;
        }

        if (((AbstractClientPlayerExt) player).getCosmeticsSource().getShouldOverrideRenderCosmeticsCheck()) {
            return true;
        }

        if (EssentialConfig.INSTANCE.getHideCosmeticsWhenServerOverridesSkin()
            && ((AbstractClientPlayerExt) player).isSkinOverrodeByServer()) {
            return false;
        }

        if (EssentialConfig.INSTANCE.getDisableCosmetics()) {
            return false;
        }

        if (suppressCosmeticRendering) {
            return false;
        }

        // If a 3rd party mod has an emulated player which doesn't override the client fields, mc.player will be null while rendering their player
        if (UMinecraft.getMinecraft().player != null) {
            final double distance = player.getDistanceToEntity(UMinecraft.getMinecraft().player);
            if (distance >= 4096D) {
                return false;
            }
        }
        return !player.isInvisible() && !player.isSpectator();
    }

    public void render(
        UMatrixStack matrixStack,
        RenderBackend.VertexConsumerProvider vertexConsumerProvider,
        @Nullable Set<EnumPart> parts,
        @NotNull AbstractClientPlayer player
    ) {
        if (!OnboardingData.hasAcceptedTos() || !EssentialConfig.INSTANCE.getEssentialEnabled()) {
            return;
        }

        final AbstractClientPlayerExt abstractClientPlayerExt = (AbstractClientPlayerExt) player;
        if (!cosmeticsShouldRender(player)) {
            return;
        }

        WearablesManager wearablesManager = abstractClientPlayerExt.getWearablesManager();
        Map<Cosmetic, ModelInstance> models = wearablesManager.getModels();
        if (models.isEmpty()) {
            return;
        }

        PlayerPose pose = PlayerPoseKt.toPose(playerRenderer);
        RenderBackend.Texture skin = new MinecraftRenderBackend.SkinTexture(
            //#if MC>=12002
            //$$ player.getSkinTextures().texture()
            //#else
            player.getLocationSkin()
            //#endif
        );

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
        if (player.isSneaking() && parts == null) {
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

        //#if MC>=12000
        //$$ World world = player.clientWorld;
        //#else
        World world = player.world;
        //#endif
        ParticleSystem particleSystem;
        if (player instanceof EmulatedUI3DPlayer.EmulatedPlayer) {
            particleSystem = ((EmulatedUI3DPlayer.EmulatedPlayer) player).getParticleSystem();
        } else if (world instanceof ParticleSystemHolder) {
            particleSystem = ((ParticleSystemHolder) world).getParticleSystem();
        } else {
            particleSystem = null;
        }
        wearablesManager.collectEvents(event -> {
            if (event instanceof ModelAnimationState.ParticleEvent) {
                if (particleSystem != null) {
                    particleSystem.spawn((ModelAnimationState.ParticleEvent) event);
                }
            } else if (event instanceof ModelAnimationState.SoundEvent) {
                boolean forceGlobal;
                State<Float> volume;
                boolean enforceEmoteSoundSettings;
                if (player instanceof EmulatedUI3DPlayer.EmulatedPlayer) {
                    EmulatedUI3DPlayer component = ((EmulatedUI3DPlayer.EmulatedPlayer) player).getEmulatedUI3DPlayer();
                    if (!component.getSounds().getUntracked()) {
                        return Unit.INSTANCE;
                    }
                    forceGlobal = true;
                    volume = component.getSoundsVolume();
                    enforceEmoteSoundSettings = false;
                } else {
                    forceGlobal = false;
                    volume = stateOf(1f);
                    enforceEmoteSoundSettings = true;
                }
                EssentialSoundManager.INSTANCE.playSound((ModelAnimationState.SoundEvent) event, forceGlobal, volume, enforceEmoteSoundSettings);
            }
            return Unit.INSTANCE;
        });

        abstractClientPlayerExt.setLastCosmeticsUpdateTime(new PlayerMolangQuery(player).getLifeTime());
    }

    @Override
    //#if MC>=11400
    //#if MC>=12102
    //$$ public void render(MatrixStack vMatrixStack, VertexConsumerProvider buffer, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
    //$$     AbstractClientPlayerEntity player = ((PlayerEntityRenderStateExt) state).essential$getEntity();
    //#else
    //$$ public void render(@NotNull MatrixStack vMatrixStack, @NotNull IRenderTypeBuffer buffer, int light, @NotNull AbstractClientPlayerEntity player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
    //#endif
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //$$     RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider(buffer, light);
    //#else
    public void doRenderLayer(@NotNull AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        UMatrixStack matrixStack = new UMatrixStack();
        RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider();
        //#endif
        render(matrixStack, vertexConsumerProvider, null, player);
    }

    //#if MC < 11400
    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
    //#endif
}
