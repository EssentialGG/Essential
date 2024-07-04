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
package gg.essential.mixins.transformers.client.gui;

import gg.essential.config.EssentialConfig;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GuiMultiplayer.class)
public abstract class Mixin_FixEnterToConnectInCustomTabs extends GuiScreen {

    //#if MC>=11600
    //$$ protected Mixin_FixEnterToConnectInCustomTabs () {
    //$$     super(null);
    //$$ }
    //#endif

    // TODO 1.16+
    //#if MC<11600
    @ModifyConstant(method = "keyTyped", constant = @Constant(intValue = 2), allow = 1)
    public int guiButtonAdjustment(int buttonIndex) {
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) {
            for (int i = 0; i < this.buttonList.size(); i++) {
                if (I18n.format("selectServer.select").equals(this.buttonList.get(i).displayString)) {
                    return i;
                }
            }
        }
        return buttonIndex;
    }
    //#endif
}
