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
import gg.essential.gui.multiplayer.DividerServerListEntry;
import gg.essential.mixins.ext.client.gui.SelectionListWithDividers;
import net.minecraft.client.gui.GuiSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

//#if MC>=11600
//$$ import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//#endif

@Mixin(GuiSlot.class)
public class Mixin_SelectionListDividers_GuiList {

    @Shadow @Final public int slotHeight;

    @ModifyExpressionValue(method = "getContentHeight", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiSlot;headerPadding:I"))
    private int modifySlotHeight$getContentHeight(int original) {
        if (this instanceof SelectionListWithDividers) {
            SelectionListWithDividers<?> ext = (SelectionListWithDividers<?>) this;
            int dividerCount = ext.getEssential$offsetDividers().size();
            return original - dividerCount * DividerServerListEntry.ENTRY_HEIGHT_DIFFERENCE;
        }
        return original;
    }

    //#if MC<=11901
    @ModifyExpressionValue(method = "func_192638_a", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiSlot;headerPadding:I"))
    private int modifySlotHeight$renderList(int originalPadding, @Local(ordinal = 5) int j) {
        if (this instanceof SelectionListWithDividers) {
            SelectionListWithDividers<?> ext = (SelectionListWithDividers<?>) this;
            int dividerCount = ext.getEssential$offsetDividers().headMap(j).size();
            return originalPadding - dividerCount * DividerServerListEntry.ENTRY_HEIGHT_DIFFERENCE;
        }
        return originalPadding;
    }
    //#endif

    //#if MC>=11600
    //$$ @ModifyExpressionValue(method = "getRowTop", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/list/AbstractList;headerHeight:I"))
    //$$ private int modifyGetRowTop(int originalPadding, int j) {
    //$$     if (this instanceof SelectionListWithDividers) {
    //$$         SelectionListWithDividers<?> ext = (SelectionListWithDividers<?>) this;
    //$$         int dividerCount = ext.getEssential$offsetDividers().headMap(j).size();
    //$$         return originalPadding - dividerCount * DividerServerListEntry.ENTRY_HEIGHT_DIFFERENCE;
    //$$     }
    //$$     return originalPadding;
    //$$ }
    //$$
    //$$ @ModifyExpressionValue(method = "getRowBottom", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/list/AbstractList;itemHeight:I"))
    //$$ private int modifyGetRowBottom(int original, int index) {
    //$$     if (this instanceof SelectionListWithDividers) {
    //$$         SelectionListWithDividers<?> ext = (SelectionListWithDividers<?>) this;
    //$$         if (ext.getEssential$offsetDividers().get(index) != null) {
    //$$             return DividerServerListEntry.DIVIDER_ENTRY_HEIGHT;
    //$$         }
    //$$     }
    //$$     return original;
    //$$ }
    //#else
    // We don't need to apply any adjustments in handleMouseInput, since GuiListExtended doesn't use elementClicked.
    //#endif

    @ModifyVariable(method = "getSlotIndexFromScreenCoords", at = @At("STORE"), ordinal = 4)
    private int modifySlotHeight$getSlotIndexFromScreenCoords(int y) {
        if (this instanceof SelectionListWithDividers) {
            SelectionListWithDividers<?> ext = (SelectionListWithDividers<?>) this;
            for (Map.Entry<Integer, ?> entry : ext.getEssential$offsetDividers().entrySet()) {
                int idx = entry.getKey();
                if (y >= idx * this.slotHeight) {
                    y += DividerServerListEntry.ENTRY_HEIGHT_DIFFERENCE;
                } else {
                    break;
                }
            }
        }
        return y;
    }
}
