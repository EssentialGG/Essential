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

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * SNI on the legacy JRE is broken if you create an unbound socket and call `connect` on it.
 * The `connect` method should add the given host to `SSLSocketImpl.serverNames` but it does not. So, to work around
 * that, we simply call `SSLSocketImpl.setHost` after creating the socket, which will add the given host to the list.
 */
public class LegacyJreSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory inner;
    private final String host;

    public LegacyJreSocketFactory(SSLSocketFactory inner, String host) {
        this.inner = inner;
        this.host = host;
    }

    private Socket configure(Socket socket) {
        try {
            socket.getClass().getDeclaredMethod("setHost", String.class)
                .invoke(socket, host);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return inner.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return inner.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return configure(inner.createSocket());
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return configure(inner.createSocket(socket, s, i, b));
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException {
        return configure(inner.createSocket(s, i));
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
        return configure(inner.createSocket(s, i, inetAddress, i1));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return configure(inner.createSocket(inetAddress, i));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return configure(inner.createSocket(inetAddress, i, inetAddress1, i1));
    }
}
