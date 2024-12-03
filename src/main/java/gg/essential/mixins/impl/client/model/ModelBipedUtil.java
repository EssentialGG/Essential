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
package gg.essential.mixins.impl.client.model;

import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.gui.emotes.EmoteWheel;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.model.util.PlayerPoseManager;
import net.minecraft.client.model.ModelBiped;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ModelBipedUtil {
    /**
     * Reset the vanilla models back to their initial pose. This is necessary because the vanilla code is not guaranteed
     * to reset all values by itself (only the ones it also modifies itself).
     * <p>
     * (Originally from {@link gg.essential.mixins.transformers.client.renderer.entity.Mixin_ApplyPoseTransform#resetPose(CallbackInfo)})
     */
    public static void resetPose(ModelBiped model) {
        ModelBipedExt ext = (ModelBipedExt) model;

        if (ext.getResetPose() == null) {
            ext.setResetPose(PlayerPoseKt.toPose(model));
        } else {
            //#if MC>=12102
            //$$ PlayerPoseKt.applyTo(ext.getResetPose(), model);
            //#else
            boolean isChild = model.isChild; // child isn't set by the method we inject into, so we need to preserve it
            PlayerPoseKt.applyTo(ext.getResetPose(), model);
            model.isChild = isChild;
            //#endif
        }
    }

    public static void applyPoseTransform(ModelBiped model, CosmeticsRenderState cState) {
        if (EmoteWheel.isPlayerArmRendering)
            return;

        cState.setPoseModified(false);

        PlayerPoseManager poseManager = cState.poseManager();
        if (poseManager == null) return;

        PlayerPose basePose = PlayerPoseKt.toPose(model);
        PlayerPose transformedPose = poseManager.computePose(cState.wearablesManager(), basePose);

        if (basePose.equals(transformedPose)) {
            return;
        }

        PlayerPoseKt.applyTo(transformedPose, model);
        cState.setPoseModified(true);
    }
}
