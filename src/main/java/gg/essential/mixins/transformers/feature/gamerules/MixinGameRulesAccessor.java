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
package gg.essential.mixins.transformers.feature.gamerules;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC<=11202
import java.util.TreeMap;
//#else
//$$ import java.util.Map;
//#endif

@Mixin(GameRules.class)
public interface MixinGameRulesAccessor {

    //#if MC<=11202
    @Accessor
    TreeMap<String, MixinGameRulesValueAccessor> getRules();
    //#else
    //$$ @Accessor
    //$$ Map<GameRules.RuleKey<?>, GameRules.RuleValue<?>> getRules();
    //#endif
}
