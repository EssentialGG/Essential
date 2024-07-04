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
package gg.essential.handlers;

import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import gg.essential.event.client.ClientTickEvent;
import gg.essential.universal.UMinecraft;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.awt.*;

public class WindowedFullscreenHandler {

    private final Minecraft mc = UMinecraft.getMinecraft();
    private boolean previousFullscreenState;
    private boolean previousFullscreenSettingState;
    private boolean exitedFullscreen = false;
    private DisplayMode displayMode;

    @Subscribe
    public void tickEvent(ClientTickEvent event) {
        if (exitedFullscreen) {
            exitedFullscreen = false;
            try {
                Display.setDisplayMode(displayMode);
                Display.setResizable(false);
                Display.setResizable(true);
            } catch (LWJGLException e) {
                Essential.logger.error("Failed to set display mode.", e);
            }
        }

        boolean currentFullscreenState = mc.isFullScreen();
        boolean newConfigState = EssentialConfig.INSTANCE.getWindowedFullscreen();
        if (currentFullscreenState != previousFullscreenState && newConfigState) {
            toggleFullscreen(currentFullscreenState);
        }

        if (previousFullscreenSettingState != newConfigState && mc.isFullScreen()) {
            if (newConfigState) {
                toggleFullscreen(true);
            } else {
                mc.toggleFullscreen();
                toggleFullscreen(false);
                mc.toggleFullscreen();
                Mouse.setCursorPosition((Display.getX() + Display.getWidth()) / 2, (Display.getY() + Display.getHeight()) / 2);
            }
        }

        previousFullscreenSettingState = newConfigState;
        previousFullscreenState = mc.isFullScreen();
    }

    public void toggleFullscreen(boolean fullscreen) {
        try {
            if (fullscreen) {
                System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
                Display.setDisplayMode(Display.getDesktopDisplayMode());
                Display.setFullscreen(false);
                Display.setLocation(0, 0);
            } else {
                System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
                displayMode = new DisplayMode(mc.displayWidth, mc.displayHeight);
                Display.setDisplayMode(displayMode);
                Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
                int x = (int) ((dimension.getWidth() - Display.getWidth()) / 2);
                int y = (int) ((dimension.getHeight() - Display.getHeight()) / 2);
                Display.setLocation(x, y);
                exitedFullscreen = true;
            }

            Display.setResizable(!fullscreen);
        } catch (LWJGLException e) {
            Essential.logger.error("Failed to update screen type.", e);
        }
    }
}

