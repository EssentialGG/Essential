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

import gg.essential.gui.multiplayer.DividerServerListEntry;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(EntryListWidget.class)
public class Mixin_SelectionListDividers_GuiMultiplayer {

    @ModifyArg(
        method = "getNeighboringEntry(Lnet/minecraft/client/gui/navigation/NavigationDirection;)Lnet/minecraft/client/gui/widget/EntryListWidget$Entry;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/EntryListWidget;getNeighboringEntry(Lnet/minecraft/client/gui/navigation/NavigationDirection;Ljava/util/function/Predicate;)Lnet/minecraft/client/gui/widget/EntryListWidget$Entry;")
    )
    public Predicate<?> modifyPredicate(Predicate<?> predicate) {
        if (!((Object)this instanceof MultiplayerServerListWidget)) return predicate;
        return predicate.and(entry -> !(entry instanceof DividerServerListEntry));
    }
}
