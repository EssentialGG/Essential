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
package gg.essential.mixins.impl.client.renderer.entity;

import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.EssentialModelRenderer;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.entity.EntityLivingBase;

public class ArmorRenderingUtil {
    /**
     * If rendering armor should be disabled based on the user's Essential settings and if cosmetics are conflicting with armor.
     * @param entity The entity which the armor and cosmetics are being rendered on
     * @param slotIndex The slot which armor is being rendered
     * @return true if cosmetics are conflicting with armor in the current slot, false if armor is okay to be rendered
     */
    public static boolean shouldDisableArmor(EntityLivingBase entity, int slotIndex) {
        if (entity instanceof AbstractClientPlayerExt) {
            AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) entity;

            int armorHidingSetting = EssentialConfig.INSTANCE.getCosmeticArmorSetting(entity);
            return armorHidingSetting == 1 && playerExt.getCosmeticsState().getPartsEquipped().contains(slotIndex) && !EssentialModelRenderer.suppressCosmeticRendering;
        }

        return false;
    }
}
