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
package gg.essential.network.connectionmanager.handler;

import com.sparkuniverse.toolbox.util.Validate;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.network.connectionmanager.ConnectionManager;
import org.jetbrains.annotations.NotNull;

public abstract class PacketHandler<IP extends Packet> {

    /**
     * When we receive the Packet we should only handle it if we have Authenticated successfully with the
     * Connection Manager itself.
     */
    protected boolean requiresAuthentication;

    public PacketHandler() {
        this(true);
    }

    //1 sec bank again
    public PacketHandler(final boolean requiresAuthentication) {
        this.requiresAuthentication = requiresAuthentication;
    }

    public Runnable handleAsync(@NotNull final ConnectionManager connectionManager, @NotNull final IP packet) {
        Validate.isTrue(
            connectionManager.isOpen(),
            () -> "Attempted to handle a Packet when the Connection Manager Connection was closed ('" + packet + "')."
        );

        return () -> {
            if (this.requiresAuthentication && !connectionManager.isAuthenticated()) {
                return;
            }

            this.onHandle(connectionManager, packet);
        };
    }

    protected abstract void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final IP packet);

}
