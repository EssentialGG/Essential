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

import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.constraints.CenterPixelConstraint
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.screenshot.DateRange
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.util.hoveredState
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.universal.UDesktop
import gg.essential.universal.UMatrixStack
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.util.ResourceLocation
import java.util.*

class ListViewComponent(val screenshotBrowser: ScreenshotBrowser) :
    ScreenshotView(View.LIST, screenshotBrowser) {

    private val numberOfItemsPerRow = BasicState(EssentialConfig.screenshotBrowserItemsPerRow).apply {
        onSetValue {
            EssentialConfig.screenshotBrowserItemsPerRow = it
        }
    }
    private val imageMap = mutableMapOf<ScreenshotId, ScreenshotPreview>()
    private val screenshotsFolder by IconButton(EssentialPalette.MC_FOLDER_8X7, tooltipText = "Screenshot Folder")
        .setDimension(IconButton.Dimension.Fixed(17f, 17f)).constrain {
            x = 10.pixels(alignOpposite = true)
            y = CenterPixelConstraint()
        }.bindParent(screenshotBrowser.titleBar, active).onLeftClick {
            UDesktop.open(screenshotFolder)
        }

    private val searchBar by EssentialCollapsibleSearchbar().constrain {
        x = SiblingConstraint(3f, alignOpposite = true)
        y = CenterPixelConstraint()
        height = 100.percent boundTo screenshotsFolder
    }.bindParent(screenshotBrowser.titleBar, active)

    private val navigation by UIContainer().constrain {
        x = contentMargin.pixels
        width = ChildBasedSizeConstraint(tabSpacing)
        height = screenshotListNavigationHeight.pixels
    } childOf this

    private val slider = ScreenshotItemsSlider(numberOfItemsPerRow, this).constrain {
        x = contentMargin.pixels(alignOpposite = true)
        y = CenterConstraint() boundTo navigation
    } childOf this

    private val selectedTab = BasicState(Tab.ALL)

    // Constrains defined in init
    private val screenshotScissorBox by HollowUIContainer()

    private val emptyScreenshotText = selectedTab.map {
        when (it) {
            Tab.ALL -> "You have no screenshots"
            Tab.WORLDS -> "No screenshots with Essential in Singleplayer"
            Tab.SERVERS -> "No screenshots with Essential in Multiplayer"
            Tab.UPLOADED -> "No screenshots uploaded"
            Tab.LIKED -> "No favorite screenshots"
        }
    }

    val screenshotScrollComponent: ScreenshotScrollComponent by ScreenshotScrollComponent(
        customScissorBoundingBox = screenshotScissorBox
    ).constrain {
        x = contentMargin.pixels
        y = screenshotListNavigationHeight.pixels
        width = 100.percent - (contentMargin * 2).pixels
        height = FillConstraint(useSiblings = false)
    }.apply {
        emptyText.bindText(emptyScreenshotText)
    } childOf this


    init {

        constrain {
            width = 100.percent
            height = 100.percent
        }
        //Adjusted for hoverOutlineWidth so that the hover outline is not scissored out of existence against the edge
        screenshotScissorBox.constrain {
            x = (CopyConstraintFloat() boundTo screenshotScrollComponent) - hoverOutlineWidth.pixels
            y = (CopyConstraintFloat() boundTo screenshotScrollComponent) - hoverOutlineWidth.pixels
            width = (CopyConstraintFloat() boundTo screenshotScrollComponent) + (hoverOutlineWidth * 2).pixels
            height = (CopyConstraintFloat() boundTo screenshotScrollComponent) + (hoverOutlineWidth * 2).pixels
        } childOf this

        for (value in Tab.values()) {
            if ((value == Tab.WORLDS || value == Tab.SERVERS)) {
                continue
            }
            TabComponent(value) childOf navigation
        }
        val percentState = BasicState(0f)
        val heightState = BasicState(0f)

        screenshotScrollComponent.addScrollAdjustEvent(false) { percent, percentageOfParent ->
            percentState.set(percent)
            heightState.set((1f / percentageOfParent) * screenshotScrollComponent.getHeight())
        }

        screenshotScissorBox.createGradient(true, 20.pixels, percentState = percentState, heightState = heightState)
        screenshotScissorBox.createGradient(false, 20.pixels, percentState = percentState, heightState = heightState)

        bindParent(screenshotBrowser.content, active)

        setupScrollbar()

        searchBar.textContent.onSetValue {
            doUpdate(selectedTab.get())
        }

    }

    override fun draw(matrixStack: UMatrixStack) {
        val providerManager = screenshotBrowser.providerManager
        providerManager.renderedLastFrame = null
        super.draw(matrixStack)
        updateTexturesFromProvider(providerManager.provide())
    }

    override fun afterInitialization() {
        super.afterInitialization()

        // Performs initial population as well as any changes to the state
        selectedTab.onSetValueAndNow {
            doUpdate(it)
        }
    }

    private fun setupScrollbar() {
        screenshotScrollComponent.setVerticalScrollBarComponent(
            screenshotBrowser.createRightDividerScroller(active).first, // Only one instance is created, so no cleanup is required
            hideWhenUseless = true,
        )
    }


    fun reload() {
        doUpdate(selectedTab.get())
    }

    private fun doUpdate(tab: Tab) {

        fun filter(metadata: ClientScreenshotMetadata?): Boolean {
            return when (tab) {
                Tab.ALL -> {
                    true
                }
                Tab.WORLDS -> {
                    metadata?.locationMetadata?.type == ClientScreenshotMetadata.Location.Type.SHARED_WORLD || metadata?.locationMetadata?.type == ClientScreenshotMetadata.Location.Type.SINGLE_PLAYER
                }
                Tab.SERVERS -> {
                    metadata?.locationMetadata?.type == ClientScreenshotMetadata.Location.Type.MULTIPLAYER
                }
                Tab.UPLOADED -> {
                    metadata?.mediaId != null
                }
                Tab.LIKED -> {
                    metadata?.favorite ?: false
                }
            }
        }
        for (screenshotDateGroup in screenshotScrollComponent.childrenOfType<ScreenshotDateGroup>()) {
            screenshotDateGroup.cleanup()
        }
        screenshotScrollComponent.clearChildren()
        imageMap.clear()

        val dateMap = TreeMap<Long, ScreenshotDateGroup>()

        for (value in DateRange.values()) {
            if (value == DateRange.MONTH_OTHER) {
                continue
            }

            val startTime = value.getStartTime()

            if (value == DateRange.EARLIER_MONTH && startTime > DateRange.LAST_WEEK.getStartTime()) {
                continue
            }

            dateMap[startTime] = ScreenshotDateGroup(value, startTime)
                .setupParent(screenshotScrollComponent, navigation)
        }

        val items = screenshotBrowser.providerManager.allPaths
            .mapNotNull {
                screenshotBrowser.providerManager.propertyMap[it]
            }
            .filter { (_, metadata) -> filter(metadata) }
            .filter { it.matchesSearch(searchBar.getText()) }
            .map { properties -> properties to getImageTime(properties, true)  }
            .sortedByDescending { (_, time) -> time }
        val otherMonthMap = HashMap<Long, ScreenshotDateGroup>()

        for ((index, propertyTime) in items.withIndex()) {
            val (property, imageTime) = propertyTime
            val preview = ScreenshotPreview(
                property,
                this,
                index,
                numberOfItemsPerRow
            )
            imageMap[property.id] = preview
            val entry = dateMap.floorEntry(imageTime.time)
            val group = if (entry != null) {
                entry.value
            } else {
                val date: Calendar = createDateOnlyCalendar(imageTime.time)
                date.set(Calendar.DAY_OF_MONTH, 1)

                otherMonthMap.computeIfAbsent(date.time.time) { time ->
                    ScreenshotDateGroup(DateRange.MONTH_OTHER, time).setupParent(screenshotScrollComponent, navigation)
                }
            }
            group.addScreenshot(preview)
        }
        screenshotScrollComponent.childrenOfType<ScreenshotDateGroup>().filter { !it.isVisible() }.forEach {
            it.cleanup()
            screenshotScrollComponent.removeChild(it)
        }
        screenshotScrollComponent.sortChildren(compareBy({
            -(it as ScreenshotDateGroup).range.ordinal
        }, {
            -(it as ScreenshotDateGroup).startTime
        }))

        // If a preview was just added to the scope, its texture will would not be set for its first draw call
        // causing it to render nothing. To prevent this behavior, the result of the underlying provide call in
        // updateItems is returned and all textures are updated accordingly.
        updateTexturesFromProvider(screenshotBrowser.providerManager.updateItems(items.map { (properties, _) -> properties.id }))
    }

    private fun updateTexturesFromProvider(providedTextures: Map<ScreenshotId, ResourceLocation>) {
        for (entry in imageMap) {
            entry.value.updateTexture(providedTextures[entry.key])
        }
    }

    fun handleRightClick(screenshotPreview: ScreenshotPreview, it: UIClickEvent) {
        screenshotBrowser.optionsDropdown.handleRightClick(
            screenshotPreview.properties,
            it,
            screenshotPreview.isScreenshotErrored.get()
        )
    }

    fun focus(properties: ScreenshotProperties) {
        USound.playButtonPress()
        screenshotBrowser.openFocusView(properties)
    }

    private inner class TabComponent(val tab: Tab) : UIContainer() {

        private val hovered = hoveredState()

        private val textColorState = selectedTab.zip(hovered).map { (selectedTab, hovered) ->
            if (selectedTab == tab) {
                EssentialPalette.ACCENT_BLUE
            } else if (hovered) {
                EssentialPalette.TEXT_HIGHLIGHT
            } else {
                EssentialPalette.TEXT
            }
        }

        private val title by EssentialUIText(tab.niceName)
            .bindShadowColor(selectedTab.map { if (it == tab) EssentialPalette.BLUE_SHADOW else EssentialPalette.COMPONENT_BACKGROUND })
            .setColor(textColorState.toConstraint()) childOf this

        init {
            constrain {
                x = SiblingConstraint(tabSpacing)
                y = CenterConstraint()
                width = ChildBasedSizeConstraint()
                height = ChildBasedSizeConstraint()
            }

            onLeftClick {
                USound.playButtonPress()
                selectedTab.set(tab)
            }
        }
    }

    private enum class Tab(val niceName: String) {
        ALL("All"),
        WORLDS("Singleplayer"),
        SERVERS("Multiplayer"),
        LIKED("Favorites"),
        UPLOADED("Uploads"),
    }
}

