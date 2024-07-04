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
package gg.essential.mixins.transformers.feature.difficulty.server;

//#if MC<=11202
import gg.essential.compatibility.vanilla.difficulty.Net;
import gg.essential.compatibility.vanilla.difficulty.UpdateDifficulty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class Mixin_BroadcastServerDifficultyChanges {
    @Inject(method = "setDifficultyForAllWorlds", at = @At("TAIL"))
    private void onDifficultyChanged(EnumDifficulty difficulty, CallbackInfo ci) {
        Net.WRAPPER.sendToAll(new UpdateDifficulty(difficulty));
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class) // Dummy target
//$$ public abstract class Mixin_BroadcastServerDifficultyChanges   {
//$$ }
//#endif
