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
package gg.essential.network.connectionmanager.sps;

import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket;
import gg.essential.util.ExtensionsKt;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handles {@code SPS_CONNECTION} telemetry.
 */
public class SPSConnectionTelemetry {

    private final UUID client;
    private final UUID sessionId;
    private final boolean relayed;

    private long sentBytes = 0;
    private int sentPackets = 0;
    private long receivedBytes = 0;
    private int receivedPackets = 0;

    private boolean sent = false;

    public SPSConnectionTelemetry(UUID client, UUID sessionId, boolean relayed) {
        this.client = client;
        this.sessionId = sessionId;
        this.relayed = relayed;
    }

    public void send() {
        if (sent) return;
        sent = true;

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("client", client);
        metadata.put("sessionId", sessionId);
        metadata.put("sentBytes", sentBytes);
        metadata.put("sentPackets", sentPackets);
        metadata.put("receivedBytes", receivedBytes);
        metadata.put("receivedPackets", receivedPackets);
        metadata.put("relayed", relayed);

        ExtensionsKt.getExecutor(Minecraft.getMinecraft())
            .execute(() -> Essential.getInstance().getConnectionManager().getTelemetryManager().enqueue(new ClientTelemetryPacket("SPS_CONNECTION", metadata)));
    }

    public void packetSent(int size) {
        sentBytes += size;
        sentPackets++;
    }

    public void packetReceived(int size) {
        receivedBytes += size;
        receivedPackets++;
    }

}
