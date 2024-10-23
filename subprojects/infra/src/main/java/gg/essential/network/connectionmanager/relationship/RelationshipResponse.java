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
package gg.essential.network.connectionmanager.relationship;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RelationshipResponse {

    @NotNull
    private final FriendRequestState friendRequestState;

    @Nullable
    private String message;

    @Nullable
    private RelationshipErrorResponse relationshipErrorResponse;

    public RelationshipResponse(@NotNull FriendRequestState friendRequestState) {
        this.friendRequestState = friendRequestState;
    }

    public RelationshipResponse(@NotNull FriendRequestState friendRequestState, @Nullable String message) {
        this.friendRequestState = friendRequestState;
        this.message = message;
    }

    public RelationshipResponse(@NotNull FriendRequestState friendRequestState, @Nullable RelationshipErrorResponse relationshipErrorResponse) {
        this.friendRequestState = friendRequestState;
        this.relationshipErrorResponse = relationshipErrorResponse;
    }

    @NotNull
    public FriendRequestState getFriendRequestState() {
        return friendRequestState;
    }

    @Nullable
    public RelationshipErrorResponse getRelationshipErrorResponse() {
        return relationshipErrorResponse;
    }

    @Nullable
    public String getRawMessage() {
        return message;
    }
}
