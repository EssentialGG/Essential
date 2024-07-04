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
package gg.essential.gui.screenshot.components

import gg.essential.Essential
import gg.essential.elementa.components.*
import gg.essential.elementa.state.State
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.util.UUIDUtil

//metadata is null when it does not exist. It is automatically created at the first time its needed (such as to set favorite or upload)
data class ScreenshotProperties(val id: ScreenshotId, var metadata: ClientScreenshotMetadata?) {

    fun matchesSearch(textSearch: String): Boolean {
        val matchAgainst = mutableListOf(id.name)
        val metadata = metadata
        if (metadata != null) {
            matchAgainst.add(UUIDUtil.getName(metadata.authorId).getNow(null) ?: "")
            val identifier = metadata.locationMetadata.identifier
            if (identifier != null) {
                val spsManager = Essential.getInstance().connectionManager.spsManager
                val host = spsManager.getHostFromSpsAddress(identifier)
                if (host != null) {
                    matchAgainst.add(UUIDUtil.getName(host).getNow(null) ?: "")
                }
                matchAgainst.add(identifier)
            }
        }
        return matchAgainst.any {
            it.contains(textSearch, ignoreCase = true)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ScreenshotProperties) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

fun ClientScreenshotMetadata.cloneWithNewChecksum(checksum: String): ClientScreenshotMetadata {
    return copy(checksum = checksum)
}

abstract class ScreenshotView(private val view: View, screenshotBrowser: ScreenshotBrowser) : UIContainer() {

    open val active: State<Boolean> = screenshotBrowser.currentView.map {
        it == view
    }

}

enum class View {
    LIST,
    FOCUS
}

enum class FocusType {
    VIEW,
    EDIT
}

// Magic constants are put here and named instead of being inlined
const val screenshotListNavigationHeight = 28
const val contentMargin = 11
const val tabSpacing = 12f
const val horizontalScreenshotPadding = 10f
const val verticalScreenshotPadding = 30f
const val hoverOutlineWidth = 2f
const val minItemsPerRow = 2
const val maxItemsPerRow = 7
const val focusImageWidthPercent = 67
const val focusImageVerticalPadding = 20