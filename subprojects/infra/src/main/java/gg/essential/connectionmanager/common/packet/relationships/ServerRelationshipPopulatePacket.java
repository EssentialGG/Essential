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
package gg.essential.connectionmanager.common.packet.relationships;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.model.relationships.Relationship;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ServerRelationshipPopulatePacket extends Packet {

    @SerializedName("a")
    @NotNull
    private final Relationship[] relationships;

    public ServerRelationshipPopulatePacket(@NotNull final Relationship relationships) {
        this(new Relationship[]{relationships});
    }

    public ServerRelationshipPopulatePacket(final Relationship[] relationships) {
        this.relationships = relationships;
    }

    @NotNull
    public Relationship[] getRelationships() {
        return relationships;
    }

    @Override
    public String toString() {
        return "ServerRelationshipPopulatePacket{" +
                "relationships=" + Arrays.toString(relationships) +
                '}';
    }
}
