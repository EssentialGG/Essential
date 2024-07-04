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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.integrated.IntegratedServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IntegratedServer.class)
public abstract class Mixin_RemoveUnsafeDifficultyChecks {
    @Redirect(method = "*", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/client/Minecraft;world:Lnet/minecraft/client/multiplayer/WorldClient;",
        opcode = Opcodes.GETFIELD
    ))
    private WorldClient getClientWorld(Minecraft client) {
        // Accessing the client world from the server is generally unsafe, so MC shouldn't do it.
        // In old versions it however does just that to sync difficulty, we have instead introduced a custom packet for
        // that job, so we can now strip the unsafe accesses.
        return null;
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_RemoveUnsafeDifficultyChecks  {
//$$ }
//#endif
