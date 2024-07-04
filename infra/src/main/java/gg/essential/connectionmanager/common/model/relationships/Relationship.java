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
package gg.essential.connectionmanager.common.model.relationships;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.relationships.enums.RelationshipState;
import com.sparkuniverse.toolbox.relationships.enums.RelationshipType;
import com.sparkuniverse.toolbox.util.DateTime;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Relationship {

    @SerializedName("a")
    @NotNull
    private final UUID senderUUID;

    @SerializedName("b")
    @NotNull
    private final UUID targetUUID;

    @SerializedName("c")
    @NotNull
    private final RelationshipType type;

    @SerializedName("d")
    @NotNull
    private final RelationshipState state;

    @SerializedName("e")
    @NotNull
    private final DateTime since;

    public Relationship(
            @NotNull final UUID senderUUID, @NotNull final UUID targetUUID, @NotNull final RelationshipType type,
            @NotNull final RelationshipState state, @NotNull final DateTime since
    ) {
        this.senderUUID = senderUUID;
        this.targetUUID = targetUUID;
        this.type = type;
        this.state = state;
        this.since = since;
    }

    @NotNull
    public UUID getSenderUUID() {
        return this.senderUUID;
    }

    @NotNull
    public UUID getTargetUUID() {
        return this.targetUUID;
    }

    @NotNull
    public RelationshipType getType() {
        return this.type;
    }

    @NotNull
    public RelationshipState getState() {
        return this.state;
    }

    @NotNull
    public DateTime getSince() {
        return this.since;
    }

    public boolean isPending() {
        return RelationshipState.PENDING == this.state;
    }

    @Override
    public String toString() {
        return "Relationship{" +
                "senderUUID=" + this.senderUUID +
                ", targetUUID=" + this.targetUUID +
                ", type=" + this.type +
                ", state=" + this.state +
                ", since=" + this.since +
                '}';
    }

}
