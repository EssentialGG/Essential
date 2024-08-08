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
package gg.essential.gui.about

import gg.essential.data.MenuData
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.about.components.*
import gg.essential.gui.elementa.essentialmarkdown.*
import gg.essential.universal.GuiScale

class AboutMenu(category: Category = Category.CHANGELOG) :
    InternalEssentialGUI(
        ElementaVersion.V6,
        "Essential",
        GuiScale.scaleForScreenSize().ordinal,
        discordActivityDescription = "Learning about Essential",
    ) {
    private var platformSpecific = BasicState(true)

    private val pages = mapOf(
        Category.CHANGELOG to ChangelogPage(Category.CHANGELOG.nameState, platformSpecific),
        Category.PRIVACY to InfoPage(MenuData.FetchableCategory.PRIVACY, "", ""),
        Category.TERMS to LinkPage(Category.TERMS.nameState, "https://essential.gg/terms-of-use"),
        Category.LICENSES to LinkPage(Category.LICENSES.nameState, "https://essential.gg/licenses"),
        Category.IMPRINT to LinkPage(Category.IMPRINT.nameState, "https://essential.gg/imprint"),
    )

    private val selectedPage = BasicState(pages[category]!!)

    // Houses the page navigation
    private val leftPane by LeftPane(pages, selectedPage, bottomDivider, outlineThickness).constrain {
        width = 25.percent
        height = 100.percent
    } childOf content

    // Middle divider
    val middleDivider by UIBlock(EssentialPalette.COMPONENT_BACKGROUND).constrain {
        x = SiblingConstraint()
        width = 100.percent boundTo rightDivider
        height = 100.percent
    } childOf content

    // Right pane houses the page content
    private val rightPane by RightPane(this, pages, selectedPage).constrain {
        x = SiblingConstraint()
        width = FillConstraint()
        height = 100.percent
    } childOf content

    // Houses the page title and top/right menu
    private val topMenu by TopMenu(this, platformSpecific, selectedPage).constrain {
        width = 100.percent
        height = 100.percent
    } childOf titleBar

    override fun updateGuiScale() {
        newGuiScale = GuiScale.scaleForScreenSize().ordinal
        super.updateGuiScale()
    }

    companion object {

        val markdownConfig: MarkdownConfig = MarkdownConfig(
            headerConfig = HeaderConfig(
                level1 = HeaderLevelConfig(EssentialPalette.TEXT, 2f, 10f, 6f),
                level2 = HeaderLevelConfig(EssentialPalette.TEXT, 1f, 8f, 3f),
                level3 = HeaderLevelConfig(EssentialPalette.TEXT, 1f, 6f, 3f),
                level4 = HeaderLevelConfig(EssentialPalette.TEXT, 1f, 6f, 3f),
                // Used only in privacy policy
                level5 = HeaderLevelConfig(EssentialPalette.TEXT_HIGHLIGHT, 1f, 14f, 16f),
                // Used only in changelogs to highlight the version number
                level6 = HeaderLevelConfig(EssentialPalette.TEXT_HIGHLIGHT, 1f, 4f, 3f),
            ),
            paragraphConfig = ParagraphConfig(
                spaceBefore = 3f,
                spaceAfter = 5f,
                softBreakIsNewline = true,
            ),
            textConfig = TextConfig(
                color = EssentialPalette.TEXT,
                shadowColor = EssentialPalette.TEXT_SHADOW_LIGHT,
            ),
            listConfig = ListConfig(
                spaceBeforeList = 0f,
                spaceAfterList = 10f,
                elementSpacingTight = 3f,
                elementSpacingLoose = 3f,
                unorderedSymbols = "■□▪▫",
            ),
            urlConfig = URLConfig(EssentialPalette.MESSAGE_SENT, EssentialPalette.MESSAGE_SENT_HOVER, underline = true),
        )
    }
}

/** Represents a page category with [nameState] as the category's name */
enum class Category(val nameState: BasicState<String>) {
    CHANGELOG(BasicState("Changelog")),
    TERMS(BasicState("Terms of Service")),
    PRIVACY(BasicState("Privacy Policy")),
    LICENSES(BasicState("Licenses")),
    IMPRINT(BasicState("Imprint")),
}
