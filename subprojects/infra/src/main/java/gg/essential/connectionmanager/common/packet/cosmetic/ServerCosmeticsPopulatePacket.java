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
package gg.essential.connectionmanager.common.packet.cosmetic;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.cosmetics.model.Cosmetic;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Sent by the server to the client to populate cosmetic data. */
public class ServerCosmeticsPopulatePacket extends Packet {

    @SerializedName("a")
    private final @NotNull List<@NotNull Cosmetic> cosmetics;

    public ServerCosmeticsPopulatePacket(@NotNull final Cosmetic cosmetic) {
        this(Collections.singletonList(cosmetic));
    }

    /**
     * @deprecated Use {@link ServerCosmeticsPopulatePacket#ServerCosmeticsPopulatePacket(Cosmetic)} or
     * {@link ServerCosmeticsPopulatePacket#ServerCosmeticsPopulatePacket(List)}.
     */
    @Deprecated
    public ServerCosmeticsPopulatePacket(@NotNull final Cosmetic... cosmetics) {
        this(Arrays.stream(cosmetics).collect(Collectors.toList()));
    }

    public ServerCosmeticsPopulatePacket(final @NotNull List<@NotNull Cosmetic> cosmetics) {
        this.cosmetics = cosmetics;
    }

    public @NotNull List<@NotNull Cosmetic> getCosmetics() {
        return this.cosmetics;
    }

}
