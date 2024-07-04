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
package gg.essential.util.swing;

import gg.essential.config.EssentialConfig;
import gg.essential.universal.UDesktop;
import gg.essential.util.swing.components.RoundBoi;
import gg.essential.util.swing.components.TitleBar;
import kotlin.Pair;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;

public class SwingUtil {
    public static final int FRAME_WIDTH = 470;
    public static final int FRAME_HEIGHT = 255;

    public static void showMessageBox(String title, String[] message) {
        Pair<JFrame, JPanel> initial = create(title);
        JPanel oops = initial.getSecond();

        int diff = ((message.length >> 1) + 1) * -15;
        for (String s : message) {
            oops.add(simpleLabel(s, diff));
            diff += 15;
        }

        JFrame frame = initial.getFirst();
        oops.add(okButton(frame));
        display(frame);
    }

    public static void showOldModCorePopup() {
        Pair<JFrame, JPanel> initial = create("Essential");
        JFrame frame = initial.getFirst();
        JPanel content = initial.getSecond();

        int diff = -30;
        for (String s : new String[]{
            "Essential has replaced ModCore!",
            "Please update all mods that use ModCore.",
            "You can find updated versions of all our mods at",
        }) {
            content.add(simpleLabel(s, diff));
            diff += 15;
        }

        JLabel mods = simpleLabel("<html><a href=\"" + "https://sk1er.club/mods" + "\"><font color=#01a552>https://sk1er.club/mods</a></html>", diff);
        mods.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mods.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    UDesktop.browse(new URI("https://sk1er.club/mods"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        content.add(mods);

        content.add(okButton(frame));
        content.add(dontShowAgain(frame));
        display(frame);
    }

    private static Pair<JFrame, JPanel> create(String title) {
        try {
            UIManager.setLookAndFeel(NimbusLookAndFeel.class.getName());
        } catch (Exception ignored) { /* ignored */ }

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setResizable(false);
        frame.setShape(new RoundRectangle2D.Double(0, 0, FRAME_WIDTH, FRAME_HEIGHT, 16, 16));
        frame.setTitle(title);

        Container container = frame.getContentPane();
        Color highlight = new Color(33, 34, 38);
        container.setBackground(highlight);
        container.add(new TitleBar(frame));

        JPanel content = new JPanel();
        content.setLayout(null);
        content.setBackground(highlight);
        content.setBounds(0, 32, FRAME_WIDTH, FRAME_HEIGHT);
        container.add(content);
        return new Pair<>(frame, content);
    }

    private static JLabel simpleLabel(String text, int pos) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(new Color(187, 187, 187));
        label.setBounds(0, (FRAME_HEIGHT >> 1) + pos, FRAME_WIDTH, 15);
        label.setFont(label.getFont().deriveFont(15f));
        return label;
    }

    private static void display(JFrame frame) {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                frame.dispose();
            }
        });
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) (screenSize.getWidth() - FRAME_WIDTH) >> 1, (int) (screenSize.getHeight() - FRAME_HEIGHT) >> 1);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    private static RoundBoi okButton(JFrame frame) {
        RoundBoi rb = new RoundBoi("Ok");
        rb.setBounds(16, (FRAME_HEIGHT / 2) + 60, FRAME_WIDTH - 256, (FRAME_WIDTH - 256) >> 2);
        Color a = new Color(1, 165, 82);
        rb.setForeground(a);
        rb.setBackground(a);
        rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rb.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                frame.dispose();
            }
        });
        return rb;
    }

    private static RoundBoi dontShowAgain(JFrame frame) {
        RoundBoi rb = new RoundBoi("Don't show again");
        rb.setBounds(240, (FRAME_HEIGHT / 2) + 60, FRAME_WIDTH - 256, (FRAME_WIDTH - 256) >> 2);
        Color a = new Color(1, 165, 82);
        rb.setForeground(a);
        rb.setBackground(a);
        rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rb.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                EssentialConfig.INSTANCE.setModCoreWarning(false);
                frame.dispose();
            }
        });
        return rb;
    }
}
