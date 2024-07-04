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
package gg.essential.gui.about.components

import gg.essential.data.VersionData
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.ChildBasedRangeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.AboutMenu
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.not
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.common.shadow.EssentialUIWrappedText
import gg.essential.gui.elementa.GuiScaleOffsetConstraint
import gg.essential.lib.gson.TypeAdapter
import gg.essential.lib.gson.annotations.JsonAdapter
import gg.essential.lib.gson.annotations.SerializedName
import gg.essential.lib.gson.stream.JsonReader
import gg.essential.lib.gson.stream.JsonWriter
import gg.essential.util.formatDate
import java.time.Instant
import java.time.ZoneId

class ChangelogComponent(private val changelog: Changelog, platformSpecific: BasicState<Boolean>) : UIContainer() {

    val version: String = changelog.version
    val data = getChangelogContent()

    init {
        constrain {
            y = SiblingConstraint(33f)
            height = ChildBasedSizeConstraint()
            width = 65.percent
        }

        // Header displays changelog version
        val headerContainer by UIContainer().constrain {
            y = SiblingConstraint(15f)
            width = 100.percent
            height = ChildBasedRangeConstraint()
        } childOf this

        val headerText by EssentialUIText("Essential Mod v$version").constrain {
            textScale = GuiScaleOffsetConstraint(1f)
            color = EssentialPalette.ACCENT_BLUE.toConstraint()
        } childOf headerContainer

        // Date to be displayed next to the Header
        val date = Instant.ofEpochMilli(changelog.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val dateText by EssentialUIText(formatDate(date), shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT).constrain {
            x = SiblingConstraint(11f)
            y = 0.pixels(alignOpposite = true) boundTo headerText
            color = EssentialPalette.TEXT.toConstraint()
        } childOf headerContainer

        val summary by EssentialUIWrappedText(changelog.summary, shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT).constrain {
            width = 100.percent
            y = SiblingConstraint(14f)
            color = EssentialPalette.TEXT.toConstraint()
        } childOf headerContainer

        // Changelogs for all platforms (and this one)
        val mainChangelog by EssentialMarkdown(
            data.first,
            AboutMenu.markdownConfig,
            disableSelection = true
        ).constrain {
            y = SiblingConstraint(14f)
            width = 100.percent
        }

        if (data.second.isNotEmpty()) {
            // Other platforms are present so bind the main logs to the platform switch's on state
            mainChangelog.bindParent(this, platformSpecific)

            // Construct and bind the other logs to the platform switch's off state
            EssentialMarkdown(data.second, AboutMenu.markdownConfig, disableSelection = true).constrain {
                y = SiblingConstraint(14f)
                width = 100.percent
            }.bindParent(this, !platformSpecific)
        } else {
            // Only this platform is present
            mainChangelog childOf this
        }
    }

    // Fetches the changelogs and splits them into data for this platform and data for all platforms
    private fun getChangelogContent(): Pair<String, String> {
        val mainLogsBuilder = StringBuilder()
        val otherLogsBuilder = StringBuilder()

        changelog.entries.forEach { platformChangelog ->

            if (platformChangelog.content.length > 1) {
                if (platformChangelog.platforms.isNullOrEmpty()) {
                    // No versions are specified in this changelog
                    otherLogsBuilder.appendLine("###### All Versions")
                    otherLogsBuilder.appendLine(platformChangelog.content)

                    mainLogsBuilder.appendLine(platformChangelog.content)
                } else {
                    // This changelog has other version data specified
                    if (platformChangelog.platforms.any { it == VersionData.getEssentialPlatform() }) {
                        // Platforms contain this version, so we'll add the changelog to the main logs
                        mainLogsBuilder.appendLine("\n" + platformChangelog.content)
                    }

                    // Build a header containing all of this changelog's platforms
                    val header = StringBuilder().append(
                        platformChangelog.platforms.joinToString { VersionData.formatPlatform(it) }
                    )

                    // Replace last comma with 'and'
                    header.lastIndexOf(",").let { index ->
                        if (index != -1) {
                            header.replace(index, index + 1, " and")
                        }
                    }

                    otherLogsBuilder.appendLine("###### $header")
                    otherLogsBuilder.appendLine("\n" + platformChangelog.content)
                }
            }
        }

        return Pair(mainLogsBuilder.toString(), otherLogsBuilder.toString())
    }

    data class Changelog(
        @SerializedName("created_at")
        val timestamp: Long,
        @SerializedName("changelog")
        val entries: List<PlatformChangelog>,
        val branches: List<String>?,
        val version: String,
        val id: String,
        val summary: String,
    )

    data class PlatformChangelog(
        @SerializedName("value")
        @JsonAdapter(ValueAdapter::class)
        val content: String,
        val platforms: List<String>?,
    ) {
        private class ValueAdapter : TypeAdapter<String>() {
            override fun write(out: JsonWriter, value: String?) {
                out.value(value)
            }

            // Elementa doesn't currently support fenced code blocks and inline code blocks look weird, so remove the backticks and syntax indicator
            override fun read(reader: JsonReader): String {
                return reader.nextString().replace(Regex("```.*?[\\s\\n\\r]"), "").replace("`", "")
            }

        }
    }
}
