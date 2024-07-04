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
package gg.essential.network.connectionmanager.ice.util;

import kotlin.collections.CollectionsKt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.RemoteCandidate;

import java.util.ArrayList;
import java.util.List;

public class CandidateUtil {
    private static final Logger LOGGER = LogManager.getLogger(CandidateUtil.class);

    public static String candidateToString(LocalCandidate candidate) {
        List<Object> components = new ArrayList<>(12);

        components.add(candidate.getFoundation());
        components.add(candidate.getParentComponent().getComponentID());
        components.add(candidate.getTransport());
        components.add(candidate.getPriority());
        components.add(candidate.getTransportAddress().getHostAddress());
        components.add(candidate.getTransportAddress().getPort());
        components.add("typ");
        components.add(candidate.getType());

        TransportAddress relatedAddress = candidate.getRelatedAddress();
        if (relatedAddress != null) {
            components.add("raddr");
            components.add(relatedAddress.getHostAddress());
            components.add("rport");
            components.add(relatedAddress.getPort());
        }

        return CollectionsKt.joinToString(components, " ", "", "", -1, "", Object::toString);
    }

    public static RemoteCandidate candidateFromString(String string, Component component) {
        try {
            String[] parts = string.split(" ");
            String foundation = parts[0];
            int componentID = Integer.parseInt(parts[1]);
            if (componentID != component.getComponentID()) {
                throw new IllegalArgumentException("Expected candidate for component id " + component.getComponentID());
            }
            Transport transport = Transport.parse(parts[2]);
            long priority = Long.parseLong(parts[3]);
            String address = parts[4];
            int port = Integer.parseInt(parts[5]);
            CandidateType type = CandidateType.parse(parts[7]);

            RemoteCandidate relatedCandidate = null;
            if (parts.length >= 12) {
                String relatedAddress = parts[9];
                int relatedPort = Integer.parseInt(parts[11]);

                TransportAddress relatedTransportAddress = new TransportAddress(relatedAddress, relatedPort, Transport.UDP);
                relatedCandidate = component.findRemoteCandidate(relatedTransportAddress);
            }

            TransportAddress transportAddress = new TransportAddress(address, port, transport);
            return new RemoteCandidate(transportAddress, component, type, foundation, priority, relatedCandidate);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse candidate \"" + string + "\":", e);
            return null;
        }
    }
}
