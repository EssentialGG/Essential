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

import gg.essential.gui.common.UI3DPlayer;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC>=11400
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//#endif

@Mixin(GuiInventory.class)
public abstract class Mixin_UI3DPlayer_Camera {

    // Forge has renamed the original method and added a tiny wrapper method in its place
    //#if FORGE && MC>=11900 && MC<11904
    //$$ private static final String DRAW_ENTITY = "renderEntityInInventoryRaw";
    //$$ private static final boolean DRAW_ENTITY_REMAP = false;
    //#else
    private static final String DRAW_ENTITY =
        //#if MC>=12005
        //$$ "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#elseif MC>=12002
        //$$ "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFILorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#elseif MC>=12000
        //$$ "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#elseif MC>=11904
        //$$ "drawEntity(Lnet/minecraft/client/util/math/MatrixStack;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#else
        "drawEntityOnScreen";
        //#endif
    private static final boolean DRAW_ENTITY_REMAP = true;
    //#endif

    @Inject(
        method = DRAW_ENTITY,
        remap = DRAW_ENTITY_REMAP,
        //#if MC>=11400
        //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRendererManager;setCameraOrientation(Lnet/minecraft/util/math/vector/Quaternion;)V", shift = At.Shift.AFTER, remap = true)
        //#else
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;setPlayerViewY(F)V", shift = At.Shift.AFTER, remap = true)
        //#endif
    )
    private static void applyUI3DPlayerCamera(
        CallbackInfo ci
        //#if MC>=12000
        //$$ , @Local(argsOnly = true) DrawContext drawContext
        //#elseif MC>=11904
        //$$ , @Local(argsOnly = true) MatrixStack matrixStack
        //#elseif MC>=11700
        //$$ , @Local(ordinal = 1) MatrixStack matrixStack
        //#elseif MC>=11400
        //$$ , @Local(ordinal = 0) MatrixStack matrixStack
        //#endif
    ) {
        UI3DPlayer component = UI3DPlayer.current;
        if (component != null) {
            UMatrixStack extraMatrixStack = component.applyCamera(Minecraft.getMinecraft().getRenderManager());
            //#if MC>=12000
            //$$ MatrixStack matrixStack = drawContext.getMatrices();
            //#endif
            //#if MC>=11400
            //$$ matrixStack.getLast().getMatrix().mul(extraMatrixStack.peek().getModel());
            //$$ matrixStack.getLast().getNormal().mul(extraMatrixStack.peek().getNormal());
            //#else
            extraMatrixStack.applyToGlobalState();
            //#endif
        }
    }
}
