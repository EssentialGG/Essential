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
import gg.essential.gui.common.UI3DPlayer;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InventoryScreen.class)
public class MixinGuiInventory_UI3DPlayerOffset {

    // Forge has renamed the original method and added a tiny wrapper method in its place
    //#if FORGE && MC>=11900 && MC<11904
    //$$ private static final String DRAW_ENTITY = "renderEntityInInventoryRaw";
    //$$ private static final boolean DRAW_ENTITY_REMAP = false;
    //#else
    private static final String DRAW_ENTITY =
        //#if MC>=11904
        //$$ "drawEntity(Lnet/minecraft/client/util/math/MatrixStack;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V";
        //#else
        "drawEntityOnScreen";
        //#endif
    private static final boolean DRAW_ENTITY_REMAP = true;
    //#endif

    //#if MC>=11904
    //$$ @ModifyExpressionValue(method = DRAW_ENTITY, at = @At(value = "CONSTANT", args = "doubleValue=1000"))
    //$$ private static double essential$modifyZOffset1(double original) {
    //#elseif MC>=11700 && MC<=11902
    //$$ @ModifyExpressionValue(method = DRAW_ENTITY, at = @At(value = "CONSTANT", args = "doubleValue=1050"), remap = DRAW_ENTITY_REMAP)
    //$$ private static double essential$modifyZOffset1(double original) {
    //#else
    @ModifyExpressionValue(method = DRAW_ENTITY, at = @At(value = "CONSTANT", args = "floatValue=1050"), remap = DRAW_ENTITY_REMAP)
    private static float essential$modifyZOffset1(float original) {
    //#endif
        if (UI3DPlayer.current != null) return 50;
        return original;
    }

    //#if MC>=11904
    //$$ @ModifyExpressionValue(method = DRAW_ENTITY, at = @At(value = "CONSTANT", args = "doubleValue=-950"))
    //$$ private static double essential$modifyZOffset2(double original) {
    //#elseif MC>=11903
    //$$ @ModifyExpressionValue(method = DRAW_ENTITY, at = @At(value = "CONSTANT", args = "floatValue=1000"), remap = DRAW_ENTITY_REMAP)
    //$$ private static float essential$modifyZOffset2(float original) {
    //#else
    @ModifyExpressionValue(method = DRAW_ENTITY, at = @At(value = "CONSTANT", args = "doubleValue=1000"), remap = DRAW_ENTITY_REMAP)
    private static double essential$modifyZOffset2(double original) {
    //#endif
        if (UI3DPlayer.current != null) return 0;
        return original;
    }

}
