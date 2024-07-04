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
package gg.essential.connectionmanager.common.packet.subscription;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

// TODO: Client/Server prefix
public class SubscriptionUpdatePacket extends Packet {

    @SerializedName("a")
    @Nullable
    private final UUID[] uuids;

    @SerializedName("b")
    private final boolean unsubscribeFromAll;

    @SerializedName("c")
    private final boolean newSubscription;

    public SubscriptionUpdatePacket(final boolean unsubscribeFromAll) {
        this(null, unsubscribeFromAll, false);
    }

    public SubscriptionUpdatePacket(@NotNull final UUID[] uuids, final boolean newSubscription) {
        this(uuids, false, newSubscription);
    }

    SubscriptionUpdatePacket(@Nullable final UUID[] uuids, final boolean unsubscribeFromAll, final boolean newSubscription) {
        this.uuids = uuids;
        this.unsubscribeFromAll = unsubscribeFromAll;
        this.newSubscription = newSubscription;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SubscriptionUpdatePacket that = (SubscriptionUpdatePacket) o;
        return this.unsubscribeFromAll == that.unsubscribeFromAll && this.newSubscription == that.newSubscription && Arrays.equals(this.uuids, that.uuids);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(this.unsubscribeFromAll, this.newSubscription);
        result = 31 * result + Arrays.hashCode(this.uuids);
        return result;
    }

    @Override
    public String toString() {
        return "SubscribeUpdateEvent{" +
                "uuids=" + Arrays.toString(this.uuids) +
                ", unsubscribeFromAll=" + this.unsubscribeFromAll +
                ", newSubscription=" + this.newSubscription +
                '}';
    }

}
