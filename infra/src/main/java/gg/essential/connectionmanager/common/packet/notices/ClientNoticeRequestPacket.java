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
package gg.essential.connectionmanager.common.packet.notices;

import gg.essential.lib.gson.annotations.SerializedName;
import com.sparkuniverse.toolbox.util.Validate;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.notices.NoticeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClientNoticeRequestPacket extends Packet {

    @SerializedName("a")
    private final @Nullable Set<@NotNull String> ids;

    @SerializedName("b")
    private final @Nullable Set<@NotNull NoticeType> types;

    @SerializedName("c")
    private final @Nullable List<@NotNull String> metadataKeys;

    @SerializedName("d")
    private final @Nullable List<@NotNull Object> metadataValues;

    public ClientNoticeRequestPacket(
            final @Nullable String id,
            final @Nullable NoticeType type,
            final @Nullable List<@NotNull String> metadataKeys,
            final @Nullable List<@NotNull Object> metadataValues
    ) {
        this(
                (id == null ? null : Collections.singleton(id)),
                (type == null ? null : Collections.singleton(type)),
                metadataKeys,
                metadataValues
        );
    }

    public ClientNoticeRequestPacket(
            final @Nullable Set<@NotNull String> ids,
            final @Nullable Set<@NotNull NoticeType> types,
            final @Nullable List<@NotNull String> metadataKeys,
            final @Nullable List<@NotNull Object> metadataValues
    ) {
        Validate.isTrue(
                !(ids == null && types == null && metadataKeys == null),
                () -> "At least one parameter should not be null or empty."
        );

        this.ids = ids;
        this.types = types;
        this.metadataKeys = metadataKeys;
        this.metadataValues = metadataValues;
    }

    public @Nullable Set<@NotNull String> getIds() {
        return this.ids;
    }

    public @Nullable Set<@NotNull NoticeType> getTypes() {
        return this.types;
    }

}
