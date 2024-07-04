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
package gg.essential.mixins.impl.client.gui;

import gg.essential.Essential;
import gg.essential.event.gui.GuiKeyTypedEvent;
import gg.essential.mixins.impl.ClassHook;
import net.minecraft.client.gui.GuiMainMenu;

public class GuiMainMenuHook extends ClassHook<GuiMainMenu> {

    public GuiMainMenuHook(GuiMainMenu instance) {
        super(instance);
    }

    //#if MC>=11400
    //$$ // We now catch keyboard events before they get to the Screen instance
    //#else
    public GuiKeyTypedEvent keyTyped(char typedChar, int keyCode) {
        GuiKeyTypedEvent event = new GuiKeyTypedEvent(instance, typedChar, keyCode);
        Essential.EVENT_BUS.post(event);
        return event;
    }
    //#endif
}
