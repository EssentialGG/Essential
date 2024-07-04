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
import gg.essential.event.client.ClientTickEvent;
import gg.essential.event.client.ReAuthEvent;
import gg.essential.universal.UMinecraft;
import gg.essential.util.USession;
import me.kbrewster.eventbus.Subscribe;

import static gg.essential.util.HelpersKt.toUSession;

public class ReAuthChecker {
    @Subscribe
    public void tick(ClientTickEvent event) {
        final String oldToken = USession.Companion.activeNow().getToken();
        final String newToken = UMinecraft.getMinecraft().getSession().getToken();
        if (!oldToken.equals(newToken)) {
            Essential.EVENT_BUS.post(new ReAuthEvent(toUSession(UMinecraft.getMinecraft().getSession())));
        }
    }
}
