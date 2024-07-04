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
package gg.essential.util;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;

public class ProtocolUtils {

    // https://en.wikipedia.org/wiki/Internet_Protocol_version_4#Header
    // The IPv4 header can vary in length, but the shortest version is most common
    private static final int IPV4_HEADER_SIZE = 20;

    // https://en.wikipedia.org/wiki/IPv6_packet#Fixed_header
    // IPv6 has a fixed header size, optionally followed by some extensions, which we'll ignore
    private static final int IPV6_HEADER_SIZE = 40;

    // https://en.wikipedia.org/wiki/User_Datagram_Protocol#UDP_datagram_structure
    private static final int UDP_HEADER_SIZE = 8;

    /**
     * Estimates the header size, in bytes, of a given {@link DatagramPacket}.
     * This estimate includes the UDP and IP header.
     * @param packet the packet
     * @return the header size, in bytes
     */
    public static int guessHeaderSize(DatagramPacket packet) {
        if (packet.getAddress() instanceof Inet4Address) {
            return IPV4_HEADER_SIZE + UDP_HEADER_SIZE;
        } else if (packet.getAddress() instanceof Inet6Address) {
            return IPV6_HEADER_SIZE + UDP_HEADER_SIZE;
        } else {
            // Best we can do here
            return UDP_HEADER_SIZE;
        }
    }

}
