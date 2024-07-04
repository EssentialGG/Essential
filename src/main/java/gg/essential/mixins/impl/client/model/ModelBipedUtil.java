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

import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.WearablesManager;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.gui.emotes.EmoteWheel;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.model.backend.PlayerPose;
import gg.essential.model.backend.minecraft.PlayerPoseKt;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.util.UUIDUtil;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
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
            boolean isChild = model.isChild; // child isn't set by the method we inject into, so we need to preserve it
            PlayerPoseKt.applyTo(ext.getResetPose(), model);
            model.isChild = isChild;
        }
    }

    public static void applyPoseTransform(ModelBiped model, Entity entity) {
        if (!(entity instanceof AbstractClientPlayerExt)) return;
        if (EmoteWheel.isPlayerArmRendering)
            return;

        AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entity;
        playerExt.setPoseModified(false);

        if (EssentialConfig.INSTANCE.getDisableEmotes() && !(entity instanceof EmulatedUI3DPlayer.EmulatedPlayer))
            return;

        WearablesManager wearablesManager = playerExt.getWearablesManager();
        PlayerPoseManager poseManager = playerExt.getPoseManager();
        if (entity.getUniqueID().equals(UUIDUtil.getClientUUID())) {
            if (
                //#if MC==11202
                model.leftArmPose == ModelBiped.ArmPose.BOW_AND_ARROW || model.rightArmPose == ModelBiped.ArmPose.BOW_AND_ARROW ||
                    model.leftArmPose == ModelBiped.ArmPose.BLOCK || model.rightArmPose == ModelBiped.ArmPose.BLOCK
                //#elseif MC==10809
                //$$ model.heldItemRight == 3 || model.heldItemLeft > 0 || model.aimedBow
                //#else
                //$$ model.leftArmPose == BipedModel.ArmPose.BOW_AND_ARROW || model.rightArmPose == BipedModel.ArmPose.BOW_AND_ARROW ||
                //$$ model.leftArmPose == BipedModel.ArmPose.THROW_SPEAR || model.rightArmPose == BipedModel.ArmPose.THROW_SPEAR ||
                //$$ model.leftArmPose == BipedModel.ArmPose.CROSSBOW_CHARGE || model.rightArmPose == BipedModel.ArmPose.CROSSBOW_CHARGE
                //#endif
            ) {
                EmoteWheel.unequipCurrentEmote();
            }
        }

        poseManager.update(wearablesManager);

        PlayerPose basePose = PlayerPoseKt.toPose(model);
        PlayerPose transformedPose = poseManager.computePose(wearablesManager, basePose);

        if (basePose.equals(transformedPose)) {
            return;
        }

        PlayerPoseKt.applyTo(transformedPose, model);
        playerExt.setPoseModified(true);
    }
}
