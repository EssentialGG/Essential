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
package gg.essential.mixins.transformers.feature.cosmetics;

import gg.essential.cosmetics.WearablesManager;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.cosmetics.events.CosmeticEventDispatcher.dispatchEvents;

@Mixin(RenderGlobal.class)
public abstract class Mixin_UpdateCosmetics {
    @Shadow
    private WorldClient world;

    //#if MC>=12102
    //$$ @Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=entities"))
    //#elseif MC>=11400
    //$$ @Inject(method = "updateCameraAndRender", at = @At(value = "CONSTANT", args = "stringValue=entities"))
    //#else
    @Inject(method = "renderEntities", at = @At("HEAD"))
    //#endif
    private void essential$updateCosmeticsPreRender(CallbackInfo ci) {
        //#if MC>=11400
        //$$ for (PlayerEntity player : this.world.getPlayers()) {
        //#else
        for (EntityPlayer player : this.world.playerEntities) {
        //#endif
            if (!(player instanceof AbstractClientPlayerExt)) {
                continue;
            }
            AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) player;

            WearablesManager wearablesManager = playerExt.getWearablesManager();
            wearablesManager.update();
            playerExt.getPoseManager().update(wearablesManager);
        }
    }

    //#if MC>=12102
    //$$ @Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=blockentities"))
    //#elseif MC>=11400
    //$$ @Inject(method = "updateCameraAndRender", at = @At(value = "CONSTANT", args = "stringValue=blockentities"))
    //#else
    @Inject(method = "renderEntities", at = @At("RETURN"))
    //#endif
    private void essential$updateCosmeticsPostRender(CallbackInfo ci) {
        //#if MC>=11400
        //$$ for (PlayerEntity player : this.world.getPlayers()) {
        //#else
        for (EntityPlayer player : this.world.playerEntities) {
        //#endif
            if (!(player instanceof AbstractClientPlayerExt)) {
                continue;
            }
            AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) player;

            WearablesManager wearablesManager = playerExt.getWearablesManager();
            wearablesManager.updateLocators(playerExt.getRenderedPose());
            dispatchEvents((AbstractClientPlayer) player, wearablesManager);
        }
    }
}
