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
package gg.essential.connectionmanager.common.packet.wardrobe;

import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.cosmetics.model.CosmeticStoreBundle;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ServerWardrobeStoreBundlePacket extends Packet {

    @SerializedName("store_bundles")
    private final @NotNull List<@NotNull CosmeticStoreBundle> storeBundles;

    public ServerWardrobeStoreBundlePacket(@NotNull List<@NotNull CosmeticStoreBundle> storeBundles) {
        this.storeBundles = storeBundles;
    }

    public @NotNull List<@NotNull CosmeticStoreBundle> getStoreBundles() {
        return storeBundles;
    }

}
