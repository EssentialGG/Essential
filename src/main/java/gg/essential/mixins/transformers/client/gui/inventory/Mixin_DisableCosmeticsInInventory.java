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
package gg.essential.mixins.transformers.client.gui.inventory;

import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.EssentialModelRenderer;
import gg.essential.gui.common.UI3DPlayer;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiInventory.class)
public class Mixin_DisableCosmeticsInInventory {

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

    @Inject(method = DRAW_ENTITY, at = @At("HEAD"))
    private static void essential$disableCosmeticsInInventoryStart(CallbackInfo info) {
        // If UI3DPlayer.current is null then the method was not called while rendering a player display
        EssentialModelRenderer.suppressCosmeticRendering = UI3DPlayer.current == null && EssentialConfig.INSTANCE.getDisableCosmeticsInInventory();
    }

    @Inject(method = DRAW_ENTITY, at = @At("RETURN"))
    private static void essential$disableCosmeticsInInventoryCleanup(CallbackInfo info) {
        EssentialModelRenderer.suppressCosmeticRendering = false;
    }
}
