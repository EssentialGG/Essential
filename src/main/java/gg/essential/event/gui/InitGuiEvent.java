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

//#if MC < 11400
import net.minecraft.client.gui.GuiButton;
//#else
//$$ import net.minecraft.client.gui.widget.Widget;
//#endif
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public class InitGuiEvent {
    private final GuiScreen screen;
    //#if MC < 11400
    private final List<GuiButton> buttonList;
    //#else
    //$$ private final List<Widget> buttonList;
    //#endif
    /**
     * Fired whenever from the GUI#Init method, this is the ideal event to add buttons to the GUI
     *
     * @param screen GuiScreen that is being initialized
     */
    //#if MC < 11400
    public InitGuiEvent(GuiScreen screen, List<GuiButton> buttonList) {
    //#else
    //$$ public InitGuiEvent(Screen screen, List<Widget> buttonList) {
    //#endif
        this.screen = screen;
        this.buttonList = buttonList;
    }

    public GuiScreen getScreen() {
        return screen;
    }

    //#if MC < 11400
    public List<GuiButton> getButtonList() {
    //#else
    //$$ public List<Widget> getButtonList() {
    //#endif
        return buttonList;
    }
}