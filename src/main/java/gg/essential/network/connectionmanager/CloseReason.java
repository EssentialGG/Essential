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

public enum CloseReason {

    SERVER_KEEP_ALIVE_TIMEOUT(4500),
    SERVER_REQUESTED(4501),
    CLIENT_FAILED_MOJANG_AUTH(4502),
    LOGIN_REQUEST_NO_RESPONSE(4503),
    INVALID_LOGIN_RESPONSE(4504),
    LOGIN_REQUEST_FAILED(4505),
    REAUTHENTICATION(4506),
    SERVER_REQUESTED_RECONNECT(4507),
    NOT_AUTHENTICATED(4508),
    USER_TOS_REVOKED(4509);

    private final int code;

    CloseReason(final int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

}
