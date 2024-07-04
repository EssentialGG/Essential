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
import gg.essential.Essential;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft doesn't use the difficulty stored in the world file when loading a world.
 * Instead, it changes all dimensions to the difficulty stored in the client's options and that's not actually updated
 * when you change the difficulty setting, so it's effectively always Normal.
 * To reproduce, just create a new world, it'll be Normal by default, change it to Peaceful, re-open the world, and
 * it'll be at Normal again.
 *
 * This mixin instead uses the difficulty stored in world file of dimension 0 (overworld), which is updated when you
 * change the difficulty, regardless of which dimension you're currently in.
 */
@Mixin(IntegratedServer.class)
public abstract class Mixin_UseDifficultyFromLevel extends MinecraftServer {
    @Redirect(method = "getDifficulty", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;difficulty:Lnet/minecraft/world/EnumDifficulty;"))
    private EnumDifficulty useLevelDifficulty(GameSettings gameSettings) {
        if (this.worlds.length == 0) {
            // fallback in case a mod calls this method before the world is loaded
            Essential.logger.warn("getDifficulty called before overworld was loaded", new IllegalStateException());
            return gameSettings.difficulty;
        }

        WorldInfo worldInfo = this.worlds[0].getWorldInfo();
        if (worldInfo == null) {
            // fallback in case a mod calls this at an odd time
            Essential.logger.error("overworld info was null during getDifficulty", new NullPointerException());
            return gameSettings.difficulty;
        }

        EnumDifficulty difficulty = worldInfo.getDifficulty();
        if (difficulty == null) {
            // fallback in case a mod calls this at an odd time
            Essential.logger.error("overworld difficulty was null during getDifficulty", new NullPointerException());
            return gameSettings.difficulty;
        }

        return difficulty;
    }

    @SuppressWarnings("ConstantConditions")
    public Mixin_UseDifficultyFromLevel() {
        //#if MC>=11200
        super(null, null, null, null, null, null, null);
        //#else
        //$$ super(null, null);
        //#endif
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_UseDifficultyFromLevel {
//$$ }
//#endif
