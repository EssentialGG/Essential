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
import net.minecraft.client.gui.GuiListExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiListExtended.class)
public class Mixin_SelectionListDividers_GuiListExtended {

    @ModifyExpressionValue(method = {"mouseClicked", "mouseReleased"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiListExtended;headerPadding:I"))
    private int modifySlotHeight$mouseInput(int original, @Local(ordinal = 3) int index) {
        if (this instanceof SelectionListWithDividers) {
            SelectionListWithDividers<?> ext = (SelectionListWithDividers<?>) this;
            int dividerCount = ext.getEssential$offsetDividers().headMap(index).size();
            return original - dividerCount * DividerServerListEntry.ENTRY_HEIGHT_DIFFERENCE;
        }
        return original;
    }

}
