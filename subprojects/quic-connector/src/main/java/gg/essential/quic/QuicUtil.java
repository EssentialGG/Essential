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
package gg.essential.quic;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class QuicUtil {

    // EM-1571: We hardcode this instead of using getLoopbackAddress due to issues with IPv6 loopback on some Windows systems
    public static final InetAddress LOCALHOST;
    static {
        try {
            LOCALHOST = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
