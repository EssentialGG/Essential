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
package gg.essential.handlers;

import gg.essential.util.HelpersKt;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public class EssentialChannelHandler {
    public static void registerEssentialChannel() {
        var id = new CustomPayload.Id<>(HelpersKt.identifier("essential:"));

        // FAPI requires us to register a S2C packet type, since when the client sends a minecraft:register packet,
        // it is declaring what channels it can receive packets on. See ClientPlayNetworking class javadoc.
        PayloadTypeRegistry.playS2C().register(id, PacketCodec.of(
            (value, buf) -> { throw new IllegalStateException("Should not be reached"); },
            buf -> { throw new IllegalStateException("Should not be reached"); }
        ));

        ClientPlayNetworking.registerGlobalReceiver(id, (packet, ctx) -> {});
    }
}
