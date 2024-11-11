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
package gg.essential.network.connectionmanager.cosmetics

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EnumPlayerModelParts

fun setCapeModelPartEnabled(enabled: Boolean) = setModelPartEnabled(EnumPlayerModelParts.CAPE, enabled)

fun setModelPartEnabled(part: EnumPlayerModelParts, enabled: Boolean) {
    val gameSettings = Minecraft.getMinecraft().gameSettings
    //#if MC>=11700
    //$$ if (gameSettings.isPlayerModelPartEnabled(part) != enabled) {
    //#else
    if (gameSettings.modelParts.contains(part) != enabled) {
        //#endif
        //#if MC>=12102
        //$$ gameSettings.setPlayerModelPart(part, enabled)
        //$$ gameSettings.sendClientSettings()
        //#else
        gameSettings.setModelPartEnabled(part, enabled)
        //#endif
    }
}
