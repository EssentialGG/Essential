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

public class GuiClickEvent extends CancellableEvent {

    private final double mouseX;
    private final double mouseY;
    private final int button;

    private final GuiScreen screen;

    /**
     * Invoked when a user clicks within a GuiScreen.
     *
     * @param mouseX x position of the mouse on click
     * @param mouseY y position of the mouse on click
     * @param button Mouse button clicked (0 = left, 1 = right, 2 = middle)
     * @param screen Screen that detected the click
     */
    public GuiClickEvent(double mouseX, double mouseY, int button, GuiScreen screen) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
        this.screen = screen;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public int getButton() {
        return button;
    }

    public GuiScreen getScreen() {
        return screen;
    }

    /** Similar to the the parent class but before other mods handle the event. */
    public static class Priority extends GuiClickEvent {
        public Priority(double mouseX, double mouseY, int button, GuiScreen screen) {
            super(mouseX, mouseY, button, screen);
        }
    }
}
