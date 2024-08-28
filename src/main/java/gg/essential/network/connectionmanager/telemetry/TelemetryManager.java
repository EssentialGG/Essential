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
package gg.essential.network.connectionmanager.telemetry;

import gg.essential.Essential;
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket;
import gg.essential.elementa.state.v2.ReferenceHolder;
import gg.essential.event.client.InitializationEvent;
import gg.essential.event.essential.TosAcceptedEvent;
import gg.essential.event.network.server.ServerJoinEvent;
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl;
import gg.essential.lib.gson.Gson;
import gg.essential.lib.gson.JsonElement;
import gg.essential.lib.gson.JsonObject;
import gg.essential.lib.gson.JsonPrimitive;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue;
import gg.essential.universal.UMinecraft;
import gg.essential.util.Multithreading;
import me.kbrewster.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import oshi.SystemInfo;
//#if MC<11701
import oshi.hardware.Processor;
//#else
//$$ import oshi.hardware.CentralProcessor;
//#endif

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static gg.essential.network.connectionmanager.telemetry.TelemetryManagerKt.*;

public class TelemetryManager implements NetworkedManager {
    @NotNull
    private final ConnectionManager connectionManager;
    @NotNull
    private final SequentialPacketQueue telemetryQueue;
    @NotNull
    private final List<ClientTelemetryPacket> packetList = new ArrayList<>();
    @NotNull
    private final ReferenceHolder referenceHolder = new ReferenceHolderImpl();

    public TelemetryManager(@NotNull final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;

        telemetryQueue = new SequentialPacketQueue.Builder(connectionManager)
            .onTimeoutSkip()
            .create();
        Essential.EVENT_BUS.register(this);

        final String bytes = System.getProperty("essential.stage2.downloaded.bytes");
        final String ms = System.getProperty("essential.stage2.downloaded.millis");

        if (StringUtils.isNumeric(bytes) && StringUtils.isNumeric(ms)) {
            try {
                enqueue(new ClientTelemetryPacket(
                    "UPDATE_DOWNLOAD_SPEED",
                    new HashMap<String, Object>() {{
                        put("downloadBytes", Integer.parseInt(bytes));
                        put("downloadMs", Integer.parseInt(ms));
                    }}
                ));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    // Adds the packet to a SequentialPacketQueue if the user is connected and authenticated
    // Otherwise, adds the packet to a list to be processed when a connection is established
    public void enqueue(@NotNull ClientTelemetryPacket packet) {
        if (connectionManager.isOpen() && connectionManager.isAuthenticated()) {
            telemetryQueue.enqueue(packet);
        } else {
            packetList.add(packet);
        }
    }

    @Override
    public void onConnected() {
        packetList.forEach(telemetryQueue::enqueue);
        packetList.clear();
    }

    @Subscribe
    private void init(InitializationEvent event) {
        setupAbFeatureTracking(this, referenceHolder);
        setupSettingsTracking(this, referenceHolder);
        ImpressionTelemetryManager.INSTANCE.initialize();

        enqueue(new ClientTelemetryPacket("LANGUAGE", new HashMap<String, Object>(){{
            put("lang", UMinecraft.getMinecraft().gameSettings.language);
        }}));
        queueInstallerTelemetryPacket();
    }

    /**
     * Called to send a piece of telemetry when the user performs a tracked action
     *
     * @param action the action the user performed.
     */
    public void clientActionPerformed(@NotNull Actions action) {
        clientActionPerformed(action, null);
    }

    /**
     * Called to send a piece of telemetry when the user performs a tracked action
     *
     * @param action the action the user performed.
     * @param context the action context (e.g. the emote activated)
     */
    public void clientActionPerformed(@NotNull Actions action, @Nullable String context) {
        enqueue(new ClientTelemetryPacket("CLIENT_ACTION", new HashMap<String, Object>() {{
            put("action", action.name());
            put("context", context == null ? "" : context); // Null context is sent as empty string due to schema restrictions on the CM
        }}));
    }

    /**
     * List of tracked actions that report to infra
     */
    public enum Actions {
        EMOTE_WHEEL_ACTIVATE,
        EMOTE_ACTIVATE,
        EMOTE_WHEEL_EDIT,
        EMOTE_WARDROBE_SECTION_VIEWED,
        CART_NOT_EMPTY_WARNING,
        PERSISTENT_TOAST_CLEARED,
        PERSISTENT_TOAST_CLICKED,
    }

    @Subscribe
    public void onServerJoin(ServerJoinEvent event) {
        UUID spsHost = connectionManager.getSpsManager().getHostFromSpsAddress(event.getServerData().serverIP);
        if (spsHost != null) {
            enqueue(new ClientTelemetryPacket("SPS_JOIN", new HashMap<String, Object>() {{
                put("host", spsHost);
            }}));
        }
    }

    @Subscribe
    public void sendHardwareAndOSTelemetry(@NotNull final TosAcceptedEvent event) {

        final Map<String, Object> hardwareMap = new HashMap<>();

        try {
            //#if MC>=11700
            //$$ CentralProcessor centralProcessor = new SystemInfo().getHardware().getProcessor();
            //$$ hardwareMap.put("cpu", centralProcessor.getProcessorIdentifier().getName());
            //#else
            Processor[] processors = new SystemInfo().getHardware().getProcessors();
            if (processors.length > 0) {
                hardwareMap.put("cpu", processors[0].getName());
            }
            //#endif
        } catch (Throwable e) {
            Essential.logger.warn("Failed to get CPU", e);
            hardwareMap.putIfAbsent("cpu", "UNKNOWN");
        }

        hardwareMap.put("gpu", GL11.glGetString(GL11.GL_RENDERER));
        hardwareMap.put("allocatedMemory", Runtime.getRuntime().maxMemory() / 1024L / 1024L);

        try {
            hardwareMap.put("os", System.getProperty("os.name", "UNKNOWN"));
            hardwareMap.put("osVersion", System.getProperty("os.version", "UNKNOWN"));
        } catch (Exception e) {
            Essential.logger.warn("Failed to get Operating System information", e);
            hardwareMap.putIfAbsent("os", "UNKNOWN");
            hardwareMap.putIfAbsent("osVersion", "UNKNOWN");
        }

        enqueue(new ClientTelemetryPacket("HARDWARE_V2", hardwareMap));
    }

    private void queueInstallerTelemetryPacket() {
        // We go async, since we are reading a file
        Multithreading.runAsync(() -> {
            try {
                Path installerMetadataPath = Essential.getInstance().getBaseDir().toPath().resolve("installer-metadata.json").toRealPath();

                if (Files.notExists(installerMetadataPath))
                    return;

                // Calculate the sha-1 checksum of the current game directory in the same way the installer does.
                byte[] pathBytes = installerMetadataPath.toString().getBytes(StandardCharsets.UTF_8);
                byte[] pathChecksumBytes = MessageDigest.getInstance("SHA-1").digest(pathBytes);
                StringBuilder pathChecksumBuilder = new StringBuilder();
                for (byte checksumByte : pathChecksumBytes) {
                    pathChecksumBuilder.append(String.format(Locale.ROOT, "%02x", checksumByte));
                }
                String pathChecksum = pathChecksumBuilder.toString();

                // Grab the raw JSON object from the telemetry file
                // This is to allow installer to add telemetry fields without having to update the mod
                String rawFile = new String(Files.readAllBytes(installerMetadataPath), StandardCharsets.UTF_8);
                JsonObject telemetryObject = new Gson().fromJson(rawFile, JsonObject.class);
                // Convert to map
                HashMap<String, Object> telemetryMap = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : telemetryObject.entrySet()) {
                    telemetryMap.put(entry.getKey(), entry.getValue());
                }
                // Check if the game folder has been moved
                boolean hasBeenMoved = false;
                Object installPathChecksum = telemetryMap.get("installPathChecksum");
                if (installPathChecksum instanceof JsonPrimitive) {
                    String installerPathChecksum = ((JsonPrimitive) installPathChecksum).getAsString();
                    hasBeenMoved = !installerPathChecksum.equals(pathChecksum);
                }

                telemetryMap.put("installPathChecksum", pathChecksum);
                telemetryMap.put("hasBeenMoved", hasBeenMoved);
                // Then queue the packet on the main thread again
                Multithreading.scheduleOnMainThread(() -> enqueue(new ClientTelemetryPacket("INSTALLER", telemetryMap)), 0, TimeUnit.SECONDS);

            } catch (Exception e) {
                Essential.logger.warn("Error when trying to parse installer telemetry!", e);
            }
        });
    }

}
