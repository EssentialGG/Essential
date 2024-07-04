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
package gg.essential.commands.impl;

import gg.essential.api.commands.DefaultHandler;
import gg.essential.api.commands.Command;
import gg.essential.gui.friends.SocialMenu;
import gg.essential.util.GuiUtil;

/**
 * Command to open friends gui
 */
public class CommandMcFriends extends Command {
    public CommandMcFriends() {
        super("essentialfriends");
    }

    @DefaultHandler
    public void handle() {
        GuiUtil.openScreen(SocialMenu.class, SocialMenu::new);
    }
}
