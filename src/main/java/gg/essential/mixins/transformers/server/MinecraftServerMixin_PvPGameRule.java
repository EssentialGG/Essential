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
package gg.essential.mixins.transformers.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.essential.Essential;
import gg.essential.handlers.EssentialGameRules;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin_PvPGameRule {

    @ModifyReturnValue(method = "isPVPEnabled", at = @At("RETURN"))
    private boolean essential$applyPvPGameRule(boolean original) {
        MinecraftServer $this = (MinecraftServer) (Object) this;

        //#if MC>=11600
        //$$ // EM-1195: some mods call isPVPEnabled before worlds (and gamerules) are loaded
        //$$ if ($this.func_241755_D_() == null) { // getOverworld()
        //$$     return original;
        //$$ }
        //#endif

        EssentialGameRules gameRules = Essential.getInstance().getGameRules();
        if (gameRules == null || gameRules.getPvpGameRule() == null) return original;
        return original && gameRules.getBoolean($this, gameRules.getPvpGameRule());
    }

}
