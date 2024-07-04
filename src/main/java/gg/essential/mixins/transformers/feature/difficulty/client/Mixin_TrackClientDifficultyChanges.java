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
package gg.essential.mixins.transformers.feature.difficulty.client;

//#if MC<=11202
import gg.essential.compatibility.vanilla.difficulty.ClientDifficultyState;
import gg.essential.compatibility.vanilla.difficulty.Net;
import gg.essential.compatibility.vanilla.difficulty.UpdateDifficulty;
import gg.essential.compatibility.vanilla.difficulty.UpdateDifficultyLock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.storage.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldInfo.class)
public abstract class Mixin_TrackClientDifficultyChanges implements ClientDifficultyState {

    @Unique
    private boolean ignoreUpdates;

    @Shadow
    private EnumDifficulty difficulty;

    @Shadow
    private boolean difficultyLocked;

    @Shadow
    public abstract void setDifficulty(EnumDifficulty p_setDifficulty_1_);

    @Shadow
    public abstract void setDifficultyLocked(boolean p_setDifficultyLocked_1_);

    @Override
    public void essential$setDifficultyFromServer(@NotNull EnumDifficulty difficulty) {
        ignoreUpdates = true;
        setDifficulty(difficulty);
        ignoreUpdates = false;
    }

    @Override
    public void essential$setDifficultyLockedFromServer(boolean locked) {
        ignoreUpdates = true;
        setDifficultyLocked(locked);
        ignoreUpdates = false;
    }

    @Inject(method = "setDifficulty", at = @At("TAIL"))
    private void onDifficultyUpdated(CallbackInfo ci) {
        if (ignoreUpdates) return;
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) return;
        if (this.difficulty == null) return; // SBA passes null for difficulty

        Net.WRAPPER.sendToServer(new UpdateDifficulty(this.difficulty));
    }

    @Inject(method = "setDifficultyLocked", at = @At("TAIL"))
    private void onDifficultyLockUpdated(CallbackInfo ci) {
        if (ignoreUpdates) return;
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) return;

        Net.WRAPPER.sendToServer(new UpdateDifficultyLock(this.difficultyLocked));
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_TrackClientDifficultyChanges   {
//$$ }
//#endif
