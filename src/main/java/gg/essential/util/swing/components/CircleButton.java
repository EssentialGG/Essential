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
package gg.essential.util.swing.components;

import javax.swing.*;
import java.awt.*;

public class CircleButton extends JButton {
    public CircleButton() {
        this.setFocusPainted(false);
        this.setContentAreaFilled(false);
    }

    @Override
    protected void paintBorder(Graphics g) {
        final Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(getForeground());
        graphics.fillOval(0, 0, getWidth(), getHeight());
        graphics.dispose();
    }
}
