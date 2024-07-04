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
package gg.essential.mixincompat;

import gg.essential.CompatMixin;
import gg.essential.CompatShadow;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Quantifier;

@CompatMixin(TargetSelector.class)
public class TargetSelectorCompat {
    @CompatShadow(original = "parse")
    public static ITargetSelector parse$original(String string, ISelectorContext context) { throw new LinkageError(); }

    // Mixin 0.7 supported target-less selectors just fine, and this patch brings that functionality back to 0.8+
    public static ITargetSelector parse(String string, ISelectorContext context) {
        if (string == null) {
            return new MemberInfo(null, Quantifier.DEFAULT);
        }
        return parse$original(string, context);
    }
}
