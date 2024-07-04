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
package com.sparkuniverse.toolbox.chat.enums;

public enum ReportVerdict {

    NO_ACTION("No action required", true),
    DELETE_MESSAGES("Delete selected messages", true),
    DELETE_USER_MESSAGES("Delete all messages by accused player in all channels (currently unavailable)", false);

    private final String displayName;
    private final boolean enabled;

    private ReportVerdict(String displayName, boolean enabled) {
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
