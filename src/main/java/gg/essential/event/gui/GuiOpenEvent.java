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
package gg.essential.event.gui;

import gg.essential.event.CancellableEvent;
import net.minecraft.client.gui.GuiScreen;

public class GuiOpenEvent extends CancellableEvent {

    private GuiScreen gui;

    /**
     * Fired whenever a GUI is opened
     *
     * @param gui The new GUI that is being opened
     */
    public GuiOpenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public GuiScreen getGui() {
        return gui;
    }

    public void setGui(GuiScreen gui) {
        this.gui = gui;
    }
}
