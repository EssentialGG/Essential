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
package gg.essential.network.connectionmanager.legacyjre;

import org.java_websocket.client.DnsResolver;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * `Inet4AddressImpl.lookupAllHostAddr` is broken on the legacy JRE (fixed with 8u65), in that it does not set the
 * `INetAddress.INetAddressHolder.originalHostName` field which is used by SSL to verify that the domain matches
 * (and if its null, that will fall back to matching against the IP and fail).
 */
public class LegacyJreDnsResolver implements DnsResolver {
    @Override
    public InetAddress resolve(URI uri) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(uri.getHost());
        try {
            Field holderField = InetAddress.class.getDeclaredField("holder");
            holderField.setAccessible(true);
            Object holder = holderField.get(address);
            Field originalHostNameField = holder.getClass().getDeclaredField("originalHostName");
            originalHostNameField.setAccessible(true);
            originalHostNameField.set(holder, uri.getHost());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }
}
