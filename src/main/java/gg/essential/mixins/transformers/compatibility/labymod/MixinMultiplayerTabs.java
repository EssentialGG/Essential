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
package gg.essential.mixins.transformers.compatibility.labymod;

//#if MC<=11202
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.labymod.gui.elements.MultiplayerTabs")
@SuppressWarnings("UnresolvedMixinReference")
public class MixinMultiplayerTabs {
    @Inject(method = "drawMultiplayerTabs", at = @At("HEAD"), remap = false)
    private static void moveTabsDown(CallbackInfo ci) {
        GlStateManager.pushMatrix();
        if (Minecraft.getMinecraft().currentScreen instanceof GuiMultiplayer) {
            GlStateManager.translate(0, 32, 0);
        }
    }

    @Inject(method = "drawMultiplayerTabs", at = @At("TAIL"), remap = false)
    private static void resetTabs(CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    @ModifyVariable(method = {"drawMultiplayerTabs", "mouseClickedMultiplayerTabs"}, at = @At("HEAD"), argsOnly = true, ordinal = 2, remap = false)
    private static int modifyMouseY(int original) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiMultiplayer) {
            return original - 32;
        }
        return original;
    }
}
//#else
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class MixinMultiplayerTabs  {
//$$ }
//#endif
