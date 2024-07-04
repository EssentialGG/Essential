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

import gg.essential.mixins.impl.client.renderer.entity.ArmorRenderingUtil;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>10809
import net.minecraft.inventory.EntityEquipmentSlot;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//#endif

/**
 * Disable armor rendering if cosmetics are conflicting.
 * <p>
 * We already have this for normal armor in {@link gg.essential.mixins.transformers.client.renderer.entity.Mixin_DisableArmorRendering},
 * but that doesn't take custom skulls or blocks in the player's HEAD equipment slot in to consideration, since they are
 * rendered separately.
 */
@Mixin(LayerCustomHead.class)
public abstract class Mixin_DisablePlayerSkullRendering {
    //#if MC>=11400
    //$$ private static final String RENDER_TARGET = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";
    //#else
    private static final String RENDER_TARGET = "doRenderLayer";
    //#endif

    @Inject(method = RENDER_TARGET, at = @At(value = "HEAD"), cancellable = true)
    private void essential$disableArmorRendering(
        //#if MC>=11400
        //$$ MatrixStack matrixStack,
        //$$ IRenderTypeBuffer buffer,
        //$$ int light,
        //#endif
        EntityLivingBase entity,
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
        //#if MC<=10809
        //$$ int headSlotIndex = 2; // The slot for HEAD is 3, but we need to remove 1 to get the index.
        //#else
        int headSlotIndex = EntityEquipmentSlot.HEAD.getIndex();
        //#endif

        if (ArmorRenderingUtil.shouldDisableArmor(entity, headSlotIndex)) {
            ci.cancel();
        }
    }
}
