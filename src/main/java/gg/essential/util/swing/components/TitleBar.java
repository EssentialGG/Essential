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

import gg.essential.util.swing.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class TitleBar extends JPanel {
    private Point initialClick;

    public TitleBar(JFrame parent) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // get location of Window
                int thisX = parent.getLocation().x;
                int thisY = parent.getLocation().y;

                // Determine how much the mouse moved since the initial click
                int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
                int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);

                // Move window to this position
                parent.setLocation(thisX + xMoved, thisY + yMoved);
            }
        });

        setLayout(null);
        setBackground(new Color(27, 28, 33));
        setBounds(0, 0, SwingUtil.FRAME_WIDTH, 32);

        JLabel title = new JLabel(parent.getTitle());
        title.setBounds(8, 8, 150, 16);
        title.setForeground(new Color(187, 187, 187));
        add(title, BorderLayout.LINE_START);

        CircleButton close = new CircleButton();
        Color buttonColour = new Color(239, 83, 80);
        close.setBackground(buttonColour);
        close.setForeground(buttonColour);
        close.setBounds(SwingUtil.FRAME_WIDTH - 32, 8, 16, 16);
        close.setFocusPainted(false);
        close.addActionListener(e -> parent.dispose());
        add(close, BorderLayout.LINE_END);
    }
}
