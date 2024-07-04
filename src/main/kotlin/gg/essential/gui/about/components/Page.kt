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

import gg.essential.Essential
import gg.essential.data.MenuData
import gg.essential.data.VersionData
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.ChildBasedRangeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.about.AboutMenu
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.not
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.universal.USound
import gg.essential.util.findParentOfType
import gg.essential.gui.util.hoveredState
import gg.essential.vigilance.utils.onLeftClick
import java.net.URI
import java.util.*

/** Represents a page with a given [name] that can be opened somehow */
abstract class Page(val name: BasicState<String>) : UIContainer() {

    init {
        constrain {
            width = 100.percent
            height = ChildBasedRangeConstraint()
        }
    }

}

/** Represents a [Page] that opens an external [link] */
class LinkPage(name: BasicState<String>, val link: String) : Page(name)

/** Represents a [Page] that opens information content in the client */
class InfoPage(name: MenuData.FetchableCategory, linkText: String = "", linkURI: String = "") : Page(name.category.nameState) {

    init {
        MenuData.INFO.get(name).whenComplete { markdown, _ ->

            val splitStrings = splitStringByLengthAndLine(markdown)
            val componentWidth = 65.percent

            // Split up the content into separate markdown components for performance
            Window.enqueueRenderOperation {
                splitStrings.forEach { string ->
                    EssentialMarkdown(string, AboutMenu.markdownConfig, disableSelection = true).constrain {
                        y = SiblingConstraint(4f)
                        width = componentWidth
                        height = ChildBasedSizeConstraint()
                    } childOf this
                }

                // Top-right link
                if (linkURI.isNotEmpty()) {
                    val link by UIWrappedText("\u00a7n${linkText.ifEmpty { linkURI }}", centered = true, lineSpacing = 12f).constrain {
                        x = 0.pixels(alignOpposite = true)
                        width = 60.pixels
                    }.onLeftClick {
                        USound.playButtonPress()
                        OpenLinkModal.openUrl(URI(linkURI))
                    } childOf this

                    link.setColor(EssentialPalette.getLinkColor(link.hoveredState()).toConstraint())
                }
            }
        }
    }

    private fun splitStringByLengthAndLine(string: String): List<String> {
        val tokenizer = StringTokenizer(string, System.lineSeparator())
        val strings: MutableList<String> = mutableListOf()
        while (tokenizer.hasMoreTokens()) {
            val line = tokenizer.nextToken().plus(System.lineSeparator()).plus(System.lineSeparator())

            if (strings.isNotEmpty()) {
                val appended = strings.last().plus(line)

                if (appended.length > 2000) {
                    strings.add(line)
                } else {
                    strings[strings.lastIndex] = strings.last().plus(line)
                }
            } else {
                strings.add(line)
            }
        }
        return strings
    }

}

/** Represents a [Page] that opens [ChangelogComponent]s in the client */
class ChangelogPage(name: BasicState<String>, private val platformSpecific: BasicState<Boolean>) : Page(name) {

    private val newestVersion = VersionData.essentialBranch
    private var nextChangelogVersion: String? = newestVersion
    private var scrolling = false

    private val divider by ColoredDivider(
        "NEW",
        textColor = EssentialPalette.TEXT_WARNING,
        dividerColor = EssentialPalette.TEXT_WARNING.darker(),
        textPadding = 10f,
    )

    private val changelogContainer by UIContainer().constrain {
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf this

    override fun afterInitialization() {
        super.afterInitialization()

        // Fetch most recent changelogs
        scrollContent(10)

        val scroller = findParentOfType<ScrollComponent>()
        scroller.addScrollAdjustEvent(isHorizontal = false) { scrollPercentage, _ ->
            // Only scroll when changelog page is visible
            if (this in parent.children) {
                if (scrollPercentage > 0.7) {
                    scrollContent()
                }
            }
        }
    }

    // Fetch data from cache and add it to the page
    private fun scrollContent(amount: Int = 1) {

        if (scrolling) return
        scrolling = true

        nextChangelogVersion?.let {
            MenuData.CHANGELOGS.get(it).thenAcceptAsync({ (version, changelog) ->
                addChangelog(changelog, nextChangelogVersion == newestVersion)
                nextChangelogVersion = version
            }, Window::enqueueRenderOperation).handleAsync({ _, exception ->
                scrolling = false

                if (exception != null) {
                    MenuData.CHANGELOGS.asMap().remove(nextChangelogVersion)
                    Essential.logger.error("An error occurred fetching the changelog for version $nextChangelogVersion", exception)
                    nextChangelogVersion = null

                    EssentialUIText("An error occurred fetching changelogs. Please check your internet connection and try again.").constrain {
                        y = SiblingConstraint(15f)
                        color = EssentialPalette.TEXT_WARNING.toConstraint()
                    } childOf this
                }

                if (amount > 0) {
                    scrollContent(amount - 1)
                }
            }, Window::enqueueRenderOperation)
        }
    }

    private fun addChangelog(log: ChangelogComponent.Changelog, isLatest: Boolean) {

        val changelog = ChangelogComponent(log, platformSpecific)

        if (changelog.data.first.isNotEmpty()) {
            // Changelog has data for this version, show it by default
            changelog childOf changelogContainer
        } else if (changelog.data.second.isNotEmpty()) {
            // Changelog only has data for other versions, only show it when platform switch is toggled on
            changelog.bindParent(changelogContainer, !platformSpecific, delayed = false, children.size)
        } else {
            // No changelog content, move on
            return
        }

        // Add the "NEW" divider indicator in the correct location
        if (!isLatest && changelog.version == VersionData.essentialVersion) {
            // Changelog is not the newest version but is the current version, meaning there's at least one newer version available
            addDivider(changelog, updateLastSeen = false)
        } else if (changelog.version == VersionData.getLastSeenChangelog()
            && VersionData.getLastSeenChangelog() != VersionData.essentialVersion) {
            // Changelog version is the last seen changelog version and last seen changelog version is not the current version
            // Meaning we've updated, so show the new divider above the previous version (this changelog) to include all new changelogs
            addDivider(changelog)
        }
    }

    private fun addDivider(changelog: ChangelogComponent, above: Boolean = true, updateLastSeen: Boolean = true) {

        // Avoid duplicate new dividers
        if (divider.hasParent) {
            divider.parent.removeChild(divider)
        }

        divider.constrain {
            y = SiblingConstraint(12f, alignOpposite = above) boundTo changelog
        } childOf this

        // If we're here, the user has loaded the new divider so update the seen changelog status
        if (updateLastSeen) {
            VersionData.updateLastSeenChangelog()
        }
    }
}
