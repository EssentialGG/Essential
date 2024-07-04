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

import gg.essential.api.commands.Command;
import gg.essential.api.commands.DefaultHandler;
import gg.essential.handlers.PauseMenuDisplay;
import gg.essential.network.connectionmanager.sps.SPSSessionSource;

public class CommandInviteFriends extends Command {
    public CommandInviteFriends() {
        super("invitefriends");
    }

    @DefaultHandler
    public void handle() {
        PauseMenuDisplay.Companion.showInviteOrHostModal(SPSSessionSource.COMMAND);
    }
}
