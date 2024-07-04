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
package gg.essential.mixins.transformers.entity.player;

import net.minecraft.entity.player.EntityPlayer;
//#if MC>10809
import net.minecraft.network.datasync.DataParameter;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayer.class)
public interface EntityPlayerAccessor {

    //#if MC>10809
    @Accessor("PLAYER_MODEL_FLAG")
    static DataParameter<Byte> getPlayerModelFlag() {
        throw new AssertionError();
    }
    //#endif
}
