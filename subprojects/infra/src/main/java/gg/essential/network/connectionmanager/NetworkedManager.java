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
package gg.essential.network.connectionmanager;

public interface NetworkedManager {
    /** Called when the connection is fully established and authenticated. */
    default void onConnected() {
        resetState();
    }

    /** Called when a connection (in any state) has been disconnected. */
    default void onDisconnect() {}

    /** Managers should implement this method to reset their state to the same as launch **/
    default void resetState() {}
}
