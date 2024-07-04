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

import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after {@link net.minecraft.client.Minecraft#currentScreen} has been set to a new screen.
 */
public class GuiOpenedEvent {
    private final @NotNull GuiScreen screen;

    public GuiOpenedEvent(@NotNull GuiScreen screen) {
        this.screen = screen;
    }

    public @NotNull GuiScreen getScreen() {
        return screen;
    }
}
