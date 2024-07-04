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
package gg.essential.mixins.transformers.compatibility.sba;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/features/tablist/TabLine.java
@Pseudo
@Mixin(targets = "codes.biscuit.skyblockaddons.features.tablist.TabLine")
@SuppressWarnings("UnresolvedMixinReference")
public abstract class MixinTabLine {
    @Shadow abstract String getText();

    // FIXME this could be better matched with MixinExtras Expressions, once they release
    @ModifyVariable(
        method = "getWidth",
        at = @At(
            value = "FIELD",
            target = "Lcodes/biscuit/skyblockaddons/features/tablist/TabStringType;PLAYER:Lcodes/biscuit/skyblockaddons/features/tablist/TabStringType;",
            shift = At.Shift.BY, by = 2
        ),
        remap = false
    )
    private int modifyWidth(int width) {

        return width;
    }
}
