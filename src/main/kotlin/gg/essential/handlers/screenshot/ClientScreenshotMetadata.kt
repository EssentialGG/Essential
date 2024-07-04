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
package gg.essential.handlers.screenshot

import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.lib.gson.annotations.SerializedName
import gg.essential.media.model.Media
import gg.essential.media.model.MediaLocationMetadata
import gg.essential.media.model.MediaLocationType
import gg.essential.network.connectionmanager.sps.SPSManager.SPS_SERVER_TLD
import java.util.*

data class ClientScreenshotMetadata(
    @SerializedName("authorId", alternate = ["a"])
    val authorId: UUID,
    @SerializedName("time", alternate = ["b"])
    val time: DateTime,
    @SerializedName("checksum", alternate = ["c"])
    val checksum: String,
    @SerializedName("editTime")
    val editTime: DateTime?,
    @SerializedName("locationMetadata", alternate = ["d"])
    val locationMetadata: Location,
    @SerializedName("favorite", alternate = ["e"])
    var favorite: Boolean,
    @SerializedName("edited", alternate = ["f"])
    var edited: Boolean,
    var mediaId: String? = null,
) {
    constructor(media: Media) : this(
        media.metadata.authorId,
        media.metadata.time,
        media.id, // we can't know the checksum for remote media, so just use something unique
        null, // unknown for remote media
        Location(media.metadata.locationMetadata),
        media.metadata.isFavorite,
        media.metadata.isEdited,
        media.id,
    )

    fun withMediaId(mediaId: String?) = copy(mediaId = mediaId)

    data class Location(
        @SerializedName("type", alternate = ["a"])
        val type: Type,
        @SerializedName("identifier", alternate = ["b"])
        val identifier: String?,
    ) {

        constructor(metadata: MediaLocationMetadata) : this(
            Type.fromNetworkType(metadata.type),
            metadata.spsHost?.let { "$it$SPS_SERVER_TLD" } ?: metadata.identifier,
        )

        enum class Type {
            SINGLE_PLAYER,
            SHARED_WORLD,
            MULTIPLAYER,
            MENU,
            UNKNOWN;

            fun toNetworkType(): MediaLocationType {
                return when (this) {
                    SINGLE_PLAYER -> MediaLocationType.SINGLE_PLAYER
                    SHARED_WORLD -> MediaLocationType.SHARED_WORLD
                    MULTIPLAYER -> MediaLocationType.MULTIPLAYER
                    MENU -> MediaLocationType.MENU
                    UNKNOWN -> MediaLocationType.UNKNOWN
                }
            }

            companion object {
                fun fromNetworkType(type: MediaLocationType): Type {
                    return when (type) {
                        MediaLocationType.SINGLE_PLAYER -> SINGLE_PLAYER
                        MediaLocationType.SHARED_WORLD -> SHARED_WORLD
                        MediaLocationType.MULTIPLAYER -> MULTIPLAYER
                        MediaLocationType.MENU -> MENU
                        MediaLocationType.UNKNOWN -> UNKNOWN
                    }
                }
            }
        }
    }
}