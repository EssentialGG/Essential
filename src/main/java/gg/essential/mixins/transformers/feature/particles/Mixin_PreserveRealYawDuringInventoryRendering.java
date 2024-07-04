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
package gg.essential.mixins.transformers.feature.particles;

import gg.essential.model.PlayerMolangQuery;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides access to the real {@link #renderYawOffset} bypassing temporary overwrites such as the one in GuiInventory.
 */
@Mixin(EntityLivingBase.class)
public class Mixin_PreserveRealYawDuringInventoryRendering implements PlayerMolangQuery.RealYawAccess {

    @Unique
    private float realRenderYawOffset;

    @Unique
    private float realPrevRenderYawOffset;

    @Shadow
    public float renderYawOffset;

    @Shadow
    public float prevRenderYawOffset;

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void essential$recordRealYaw(CallbackInfo ci) {
        realRenderYawOffset = this.renderYawOffset;
        realPrevRenderYawOffset = this.prevRenderYawOffset;
    }

    @Override
    public float essential$getRealRenderYaw() {
        return realRenderYawOffset;
    }

    @Override
    public float essential$getRealPrevRenderYaw() {
        return realPrevRenderYawOffset;
    }
}
