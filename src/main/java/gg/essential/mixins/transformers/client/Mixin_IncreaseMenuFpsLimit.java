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
package gg.essential.mixins.transformers.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC>=12102
//$$ @Mixin(net.minecraft.client.option.InactivityFpsLimiter.class)
//#else
@Mixin(net.minecraft.client.Minecraft.class)
//#endif
public abstract class Mixin_IncreaseMenuFpsLimit {
    //#if MC>=12102
    //$$ @ModifyConstant(method = "update", constant = @Constant(intValue = 60))
    //#elseif MC>=11400
    //$$ @ModifyConstant(method = "getFramerateLimit", constant = @Constant(intValue = 60))
    //#else
    @ModifyConstant(method = "getLimitFramerate", constant = @Constant(intValue = 30))
    //#endif
    public int modify(int value) {
        return 144;
    }
}
