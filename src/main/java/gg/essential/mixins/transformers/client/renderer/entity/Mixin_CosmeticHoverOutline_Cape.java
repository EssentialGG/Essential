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

import gg.essential.cosmetics.CosmeticsState;
import gg.essential.cosmetics.EquippedCosmetic;
import gg.essential.gui.common.CosmeticHoverOutlineEffect;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.cosmetics.Cosmetic;
import kotlin.Unit;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.mod.cosmetics.CapeDisabledKt.CAPE_DISABLED_COSMETIC;

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.entity.layers.LayerRenderer;
//$$ import net.minecraft.client.renderer.entity.model.PlayerModel;
//#endif

// For fallback renderer see renderThirdPartyCapeForHoverOutline
@Mixin(LayerCape.class)
public abstract class Mixin_CosmeticHoverOutline_Cape
    //#if MC>=11600
    //$$ extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>>
    //#endif
{

    //#if MC>=11600
    //$$ private static final String RENDER_LAYER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFFFFF)V";
    //#else
    private static final String RENDER_LAYER = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V";
    //#endif

    @Unique
    private boolean outlinePass;

    @Inject(method = RENDER_LAYER, at = @At("TAIL"))
    private void renderIntoHoverOutlineFrameBuffer(
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //$$ IRenderTypeBuffer buffer,
        //$$ int light,
        //#endif
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        //#if MC<11400
        float scale,
        //#endif
        CallbackInfo ci
    ) {
        CosmeticHoverOutlineEffect outlineEffect = CosmeticHoverOutlineEffect.Companion.getActive();
        if (outlineEffect == null) {
            return;
        }

        AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) player;
        CosmeticsState cosmeticsState = playerExt.getCosmeticsState();
        EquippedCosmetic equippedCosmetic = cosmeticsState.getCosmetics().get(CosmeticSlot.CAPE);
        Cosmetic cosmetic = equippedCosmetic != null ? equippedCosmetic.getCosmetic() : null;
        if (cosmetic == null) {
            cosmetic = CAPE_DISABLED_COSMETIC;
        }

        if (!outlinePass) {
            outlinePass = true;

            //#if MC>=11600
            //$$ if (buffer instanceof IRenderTypeBuffer.Impl) ((IRenderTypeBuffer.Impl) buffer).finish();
            //#endif

            outlineEffect.allocOutlineBuffer(cosmetic).use(() -> {
                //#if MC>=11400
                //$$ render(matrixStack, buffer, light, player, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                //#else
                doRenderLayer(player, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
                //#endif

                //#if MC>=11600
                //$$ if (buffer instanceof IRenderTypeBuffer.Impl) ((IRenderTypeBuffer.Impl) buffer).finish();
                //#endif

                return Unit.INSTANCE;
            });

            outlinePass = false;
        }
    }

    @Shadow
    //#if MC>=11600
    //$$ public abstract void render(
    //#else
    public abstract void doRenderLayer(
    //#endif
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //$$ IRenderTypeBuffer buffer,
        //$$ int light,
        //#endif
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
        //#if MC<11400
        , float scale
        //#endif
    );

    //#if MC>=11600
    //$$ Mixin_CosmeticHoverOutline_Cape() { super(null); }
    //#endif
}
