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

public enum ChannelType {

    /**
     * A text channel with no members but every user can read the channel message history.
     */
    ANNOUNCEMENT(0),

    /**
     * A text channel with a lot of recipients.
     */
    TEXT(0),

    /**
     * A direct message between two users.
     */
    DIRECT_MESSAGE(2),

    /**
     * A direct message between multiple users.
     */
    GROUP_DIRECT_MESSAGE(10);

    private final int baseUserLimit;

    ChannelType(final int baseUserLimit) {
        this.baseUserLimit = baseUserLimit;
    }

    public int getBaseUserLimit() {
        return this.baseUserLimit;
    }

    public boolean hasUserLimit() {
        return this.baseUserLimit > 0;
    }

}
