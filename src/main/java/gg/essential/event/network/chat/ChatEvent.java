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
package gg.essential.event.network.chat;

import gg.essential.event.CancellableEvent;
import net.minecraft.util.text.ITextComponent;

public class ChatEvent extends CancellableEvent {

    private final ITextComponent chat;

    /**
     * Called whenever a chat message is sent
     *
     * @param chat The message that was sent.
     */
    public ChatEvent(ITextComponent chat) {
        this.chat = chat;
    }

    public ITextComponent getChat() {
        return chat;
    }
}
