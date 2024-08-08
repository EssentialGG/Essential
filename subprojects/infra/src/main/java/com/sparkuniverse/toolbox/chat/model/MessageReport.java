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
package com.sparkuniverse.toolbox.chat.model;

import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageReport {

    @SerializedName("a")
    private final long id;

    @SerializedName("b")
    private final long channelId;

    @SerializedName("c")
    private final long messageId;

    @SerializedName("d")
    @NotNull
    private final String reason;

    @SerializedName("e")
    @NotNull
    private final CreatedInfo createdInfo;

    @SerializedName("f")
    private final boolean closed;

    @SerializedName("g")
    @Nullable
    private final ReportVerdict reportVerdict;

    @Deprecated
    public MessageReport(
            final long id, final long channelId, final long messageId, @NotNull final String reason,
            @NotNull final CreatedInfo createdInfo, final boolean closed
    ) {
        this(id, channelId, messageId, reason, createdInfo, closed, null);
    }

    public MessageReport(
            final long id, final long channelId, final long messageId, @NotNull final String reason,
            @NotNull final CreatedInfo createdInfo, final boolean closed, @Nullable final ReportVerdict reportVerdict
    ) {
        this.id = id;
        this.channelId = channelId;
        this.messageId = messageId;
        this.reason = reason;
        this.createdInfo = createdInfo;
        this.closed = closed;
        this.reportVerdict = reportVerdict;
    }

    public long getId() {
        return this.id;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public long getMessageId() {
        return this.messageId;
    }

    @NotNull
    public String getReason() {
        return this.reason;
    }

    @NotNull
    public CreatedInfo getCreatedInfo() {
        return this.createdInfo;
    }

    public boolean isClosed() {
        return closed;
    }

}
