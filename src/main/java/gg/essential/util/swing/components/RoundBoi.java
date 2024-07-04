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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class RoundBoi extends JButton {
    private Color inner = new Color(33, 34, 38);
    private final Font font;
    private final String textStr;

    public RoundBoi(String buttonText) {
        setFocusPainted(false);
        setContentAreaFilled(false);
        textStr = buttonText;
        font = new JLabel().getFont().deriveFont(15f).deriveFont(Font.BOLD);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                inner = new Color(1, 165, 82);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                inner = new Color(33, 34, 38);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(getForeground());
        graphics.fill(new RoundRectangle2D.Double(0, 0, getBounds().width, getBounds().height, 16, 16));
        graphics.setColor(inner);
        graphics.fill(new RoundRectangle2D.Double(3, 3, getBounds().width - 6, getBounds().height - 6, 16, 16));
        graphics.setFont(font);
        final FontMetrics fm = graphics.getFontMetrics();
        int x = (getBounds().width - fm.stringWidth(textStr)) >> 1;
        int y = ((getBounds().height - fm.getHeight()) >> 1) + fm.getAscent();
        graphics.setColor(new Color(187, 187, 187));
        graphics.drawString(textStr, x, y);
        graphics.dispose();
    }
}
