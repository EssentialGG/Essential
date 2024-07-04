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
package gg.essential.event.client;

import gg.essential.util.USession;
import org.jetbrains.annotations.NotNull;

public class ReAuthEvent {
    private final USession session;

    public ReAuthEvent(@NotNull USession session) {
        this.session = session;
    }

    public USession getSession() {
        return this.session;
    }
}
