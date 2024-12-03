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
import dev.folomeev.kotgl.matrix.matrices.Mat4;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.EssentialModelRenderer;
import gg.essential.gui.emotes.EmoteWheel;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.mixins.impl.client.renderer.entity.PlayerEntityRendererExt;
import gg.essential.mod.cosmetics.SkinLayer;
import gg.essential.model.EnumPart;
import gg.essential.model.backend.RenderBackend;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.universal.UMatrixStack;
import gg.essential.util.GLUtil;
import net.minecraft.client.model.ModelPlayer;
import gg.essential.handlers.RenderPlayerBypass;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.Set;

//#if MC>=12102
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//$$ import net.minecraft.util.Identifier;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.render.entity.EntityRendererFactory.Context;
//$$ import net.minecraft.text.Text;
//#endif

//#if MC>=11400
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.model.ModelRenderer;
//#else
//#endif

//#if MC<=10809
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//$$ import net.minecraft.entity.Entity;
//#else
import net.minecraft.client.renderer.entity.RenderLivingBase;
//#endif

//#if FORGE
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
//#endif

@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer
    //#if MC>=12102
    //$$ extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel>
    //#elseif MC>=11400
    //$$ extends LivingRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>>
    //#elseif MC>=12000
    extends RenderLivingBase<AbstractClientPlayer>
    //#else
    //$$ extends RendererLivingEntity<AbstractClientPlayer>
    //#endif
    implements PlayerEntityRendererExt
{

    //#if MC<11400
    @Shadow public abstract ModelPlayer getMainModel();
    //#endif

    @Unique
    protected EssentialModelRenderer essentialModelRenderer;

    //#if MC>=11700
    //$$ @Inject(method = "<init>", at = @At("RETURN"))
    //#else
    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/RenderManager;Z)V", at = @At("RETURN"))
    //#endif
    private void initEssentialCosmeticsLayer(CallbackInfo ci) {
        essentialModelRenderer = new EssentialModelRenderer((RenderPlayer) (Object) this);
        this.layerRenderers.add(essentialModelRenderer);
    }

    @Override
    public Iterable<?> essential$getFeatures() {
        return this.layerRenderers;
    }

    //#if MC>=12102
    //$$ @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("RETURN"))
    //$$ private void disableOuterLayerWhereCoveredByCosmetic(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
    //$$     Set<SkinLayer> coveredLayers = ((AbstractClientPlayerExt) player).getCosmeticsState().getCoveredLayers();
    //$$     state.hatVisible &= !coveredLayers.contains(SkinLayer.HAT);
    //$$     state.jacketVisible &= !coveredLayers.contains(SkinLayer.JACKET);
    //$$     state.leftSleeveVisible &= !coveredLayers.contains(SkinLayer.LEFT_SLEEVE);
    //$$     state.rightSleeveVisible &= !coveredLayers.contains(SkinLayer.RIGHT_SLEEVE);
    //$$     state.leftPantsLegVisible &= !coveredLayers.contains(SkinLayer.LEFT_PANTS_LEG);
    //$$     state.rightPantsLegVisible &= !coveredLayers.contains(SkinLayer.RIGHT_PANTS_LEG);
    //$$ }
    //#else
    @Inject(method = "setModelVisibilities", at = @At("RETURN"))
    private void disableOuterLayerWhereCoveredByCosmetic(AbstractClientPlayer player, CallbackInfo ci) {
        Set<SkinLayer> coveredLayers = ((AbstractClientPlayerExt) player).getCosmeticsState().getCoveredLayers();
        ModelPlayer model = getMainModel();
        model.bipedHeadwear.showModel &= !coveredLayers.contains(SkinLayer.HAT);
        model.bipedBodyWear.showModel &= !coveredLayers.contains(SkinLayer.JACKET);
        model.bipedLeftArmwear.showModel &= !coveredLayers.contains(SkinLayer.LEFT_SLEEVE);
        model.bipedRightArmwear.showModel &= !coveredLayers.contains(SkinLayer.RIGHT_SLEEVE);
        model.bipedLeftLegwear.showModel &= !coveredLayers.contains(SkinLayer.LEFT_PANTS_LEG);
        model.bipedRightLegwear.showModel &= !coveredLayers.contains(SkinLayer.RIGHT_PANTS_LEG);
    }
    //#endif

    //#if FORGE && MC<11700
    @Redirect(
            //#if MC>=11400
            //$$ method = "render(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false)
            //#else
            method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", remap = false)
            //#endif
    )
    private boolean cancelPostIfBypass(EventBus eventBus, Event event) {
        return !RenderPlayerBypass.bypass && eventBus.post(event);
    }
    //#endif

    @Inject(method = "renderLeftArm", at = @At("HEAD"))
    private void isRenderingLeftArm(CallbackInfo ci) {
        EmoteWheel.isPlayerArmRendering = true;
    }

    @Inject(method = "renderLeftArm", at = @At("RETURN"))
    //#if MC>=11400
    //#if MC>=12102
    //$$ private void renderLeftArm(MatrixStack vMatrixStack, VertexConsumerProvider buffers, int combinedLight, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
    //$$     AbstractClientPlayerEntity player = MinecraftClient.getInstance().player;
    //$$     if (player == null) return;
    //#else
    //$$ private void renderLeftArm(MatrixStack vMatrixStack, IRenderTypeBuffer buffers, int combinedLight, AbstractClientPlayerEntity player, CallbackInfo ci) {
    //#endif
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //$$     RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider(buffers, combinedLight);
    //#else
    private void renderLeftArm(AbstractClientPlayer player, CallbackInfo ci) {
        UMatrixStack matrixStack = new UMatrixStack();
        RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider();
    //#endif
        //#if MC<12102
        getMainModel().isChild = false;
        //#endif
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
        essentialModelRenderer.render(matrixStack, vertexConsumerProvider, cState, EnumSet.of(EnumPart.LEFT_ARM));
        EmoteWheel.isPlayerArmRendering = false;
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"))
    private void isRenderingRightArm(CallbackInfo ci) {
        EmoteWheel.isPlayerArmRendering = true;
    }

    @Inject(method = "renderRightArm", at = @At("RETURN"))
    //#if MC>=11400
    //#if MC>=12102
    //$$ private void renderRightArm(MatrixStack vMatrixStack, VertexConsumerProvider buffers, int combinedLight, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
    //$$     AbstractClientPlayerEntity player = MinecraftClient.getInstance().player;
    //$$     if (player == null) return;
    //#else
    //$$ private void renderRightArm(MatrixStack vMatrixStack, IRenderTypeBuffer buffers, int combinedLight, AbstractClientPlayerEntity player, CallbackInfo ci) {
    //#endif
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //$$     RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider(buffers, combinedLight);
    //#else
    private void renderRightArm(AbstractClientPlayer player, CallbackInfo ci) {
        UMatrixStack matrixStack = new UMatrixStack();
        RenderBackend.VertexConsumerProvider vertexConsumerProvider = new MinecraftRenderBackend.VertexConsumerProvider();
    //#endif
        //#if MC<12102
        getMainModel().isChild = false;
        //#endif
        CosmeticsRenderState cState = new CosmeticsRenderState.Live(player);
        essentialModelRenderer.render(matrixStack, vertexConsumerProvider, cState, EnumSet.of(EnumPart.RIGHT_ARM));
        EmoteWheel.isPlayerArmRendering = false;
    }

    //#if MC>=12102
    //$$ @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 1))
    //#elseif MC>=12005
    //$$ @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", ordinal = 1))
    //#elseif MC>=11600
    //$$ @Inject(method = "renderName(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;Lnet/minecraft/util/text/ITextComponent;Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingRenderer;renderName(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/text/ITextComponent;Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V", ordinal = 1))
    //#elseif MC>=11200
    @Inject(method = "renderEntityName", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderLivingBase;renderEntityName(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;D)V"))
    //#else
    //$$ @Inject(method = "renderOffsetLivingLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderOffsetLivingLabel(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V"))
    //#endif
    //#if MC!=11202
    //$$ private void setNametagEntity(CallbackInfo ci) {
    //$$     OnlineIndicator.currentlyDrawingEntityName.set(true);
    //$$ }
    //#else
    private void setNametagEntity(CallbackInfo ci, @Local(argsOnly = true) AbstractClientPlayer entityIn) {
        OnlineIndicator.nametagEntity = entityIn;
    }
    //#endif

    @Override
    public Mat4 essential$getTransform(AbstractClientPlayer player, float yaw, float partialTicks) {
        // FIXME 1.21.2 this is getting unreasonably expensive
        //#if MC>=11400
        //$$ MatrixStack stack = new MatrixStack();
        //#if MC>=12102
        //$$ PlayerEntityRenderState state = this.createRenderState();
        //$$ this.updateRenderState(player, state, partialTicks);
        //$$ this.setupTransforms(state, stack, state.bodyYaw, state.baseScale);
        //#else
        //$$ this.applyRotations(
        //$$     player, stack, player.ticksExisted + partialTicks, yaw, partialTicks
            //#if MC>=12006
            //$$ , player.getScale()
            //#endif
        //$$ );
        //#endif
        //$$ return GLUtil.INSTANCE.glGetMatrix(stack, 1f);
        //#else
        return new UMatrixStack().runReplacingGlobalState(() -> {
            this.applyRotations(player, player.ticksExisted + partialTicks, yaw, partialTicks);
            return GLUtil.INSTANCE.glGetMatrix(1f);
        });
        //#endif
    }

    //#if MC<12102
    @Shadow protected abstract void applyRotations(
        AbstractClientPlayer player,
        //#if MC>=11400
        //$$ MatrixStack stack,
        //#endif
        float lifeTime,
        float yaw,
        float partialTicks
        //#if MC>=12006
        //$$ , float scale
        //#endif
    );
    //#endif

    public MixinRenderPlayer() { super(null, null, 0f); }
}
