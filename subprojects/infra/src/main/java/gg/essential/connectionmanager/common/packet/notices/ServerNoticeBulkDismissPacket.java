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


import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class ServerNoticeBulkDismissPacket extends Packet {

    @SerializedName("dismissed_notice_ids")
    private final @NotNull Set<String> noticeIds;

    @SerializedName("errors")
    private final @NotNull List<ErrorDetails> errors;

    public ServerNoticeBulkDismissPacket(final @NotNull Set<String> noticeIds, final @NotNull List<ErrorDetails> errors) {
        this.noticeIds = noticeIds;
        this.errors = errors;
    }

    public @NotNull Set<String> getNoticeIds() {
        return this.noticeIds;
    }

    public @NotNull List<ErrorDetails> getErrors() {
        return this.errors;
    }

    public static class ErrorDetails {

        @SerializedName("notice_id")
        private final @NotNull String noticeId;

        @SerializedName("reason")
        private final @NotNull String reason;

        public ErrorDetails(final @NotNull String noticeId, final @NotNull String reason) {
            this.noticeId = noticeId;
            this.reason = reason;
        }

        public @NotNull String getNoticeId() {
            return noticeId;
        }

        public @NotNull String getReason() {
            return reason;
        }

    }

}