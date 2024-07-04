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
package gg.essential.network.connectionmanager.serverdiscovery;

import com.google.common.collect.Maps;
import gg.essential.connectionmanager.common.packet.serverdiscovery.ClientServerDiscoveryRequestPopulatePacket;
import gg.essential.connectionmanager.common.packet.serverdiscovery.ServerServerDiscoveryPopulatePacket;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.serverdiscovery.handler.ServerServerDiscoveryPopulatePacketHandler;
import gg.essential.serverdiscovery.model.ServerDiscovery;
import gg.essential.util.MinecraftUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class ServerDiscoveryManager implements NetworkedManager, Iterable<ServerDiscovery> {

    @NotNull
    private final Map<String, ServerDiscovery> servers = Maps.newConcurrentMap();
    private Map<String, ServerDiscovery> serversByAddress;
    private Map<Pattern, ServerDiscovery> serversByRegexAddress;
    private final ConnectionManager connectionManager;

    public ServerDiscoveryManager(@NotNull final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        connectionManager.registerPacketHandler(ServerServerDiscoveryPopulatePacket.class, new ServerServerDiscoveryPopulatePacketHandler(this));
    }

    @NotNull
    public Map<String, ServerDiscovery> getServers() {
        return this.servers;
    }

    @Nullable
    public ServerDiscovery getServer(@Nullable final String id) {
        return this.servers.get(id);
    }

    public void addServers(@NotNull final Collection<ServerDiscovery> servers) {
        for (@NotNull final ServerDiscovery serverDiscovery : servers) {
            this.addServer(serverDiscovery);
        }
    }

    public void addServer(@NotNull final ServerDiscovery serverDiscovery) {
        this.servers.put(serverDiscovery.getId(), serverDiscovery);
        this.serversByAddress = null;
        this.serversByRegexAddress = null;
    }

    @NotNull
    @Override
    public Iterator<ServerDiscovery> iterator() {
        return this.servers.values().iterator();
    }

    @Nullable
    public ServerDiscovery findServerByAddress(final String address) {
        if (this.serversByAddress == null || this.serversByRegexAddress == null) {
            this.serversByAddress = Maps.newHashMap();
            this.serversByRegexAddress = Maps.newHashMap();
            for (ServerDiscovery server : this.servers.values()) {
                for (String serverAddress : server.getAddresses()) {
                    if (serverAddress.startsWith("^") && serverAddress.endsWith("$")) {
                        this.serversByRegexAddress.put(Pattern.compile(serverAddress), server);
                    } else {
                        this.serversByAddress.put(serverAddress, server);
                    }
                }
            }
        }

        ServerDiscovery exactMatch = this.serversByAddress.get(address);
        if (exactMatch != null) {
            return exactMatch;
        }

        for (Map.Entry<Pattern, ServerDiscovery> entry : this.serversByRegexAddress.entrySet()) {
            if (entry.getKey().matcher(address).matches()) {
                return entry.getValue();
            }
        }

        return null;
    }

    @NotNull
    public String normalizeAddress(@NotNull String address) {
        ServerDiscovery serverDiscovery = findServerByAddress(address);
        return serverDiscovery != null ? serverDiscovery.getAddresses().get(0) : address;
    }

    @Override
    public void onConnected() {
        resetState();
        connectionManager.send(new ClientServerDiscoveryRequestPopulatePacket(MinecraftUtils.getCurrentProtocolVersion()));
    }

    @Override
    public void resetState() {
        this.servers.clear();
        this.serversByAddress = null;
        this.serversByRegexAddress = null;
    }

}
