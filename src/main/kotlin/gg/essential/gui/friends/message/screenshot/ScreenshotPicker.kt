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
package gg.essential.gui.friends.message.screenshot

import gg.essential.config.EssentialConfig
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialCollapsibleSearchbar
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.fillRemainingHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.scrollable
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.screenshot.DateRange
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.network.connectionmanager.media.ScreenshotCollectionChangeEvent
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.universal.USound
import gg.essential.util.createDateOnlyCalendar
import gg.essential.util.getImageTime
import gg.essential.util.scrollGradient
import gg.essential.vigilance.utils.onLeftClick
import java.util.*
import java.util.function.Consumer

class ScreenshotPicker(
    private val screenshotAttachmentManager: ScreenshotAttachmentManager
) : UIContainer() {

    private val navigationMenuHeight = 30f
    private val numberOfItemsPerRow = mutableStateOf(EssentialConfig.screenshotBrowserItemsPerRow).apply {
        onSetValue(this@ScreenshotPicker) {
            EssentialConfig.screenshotBrowserItemsPerRow = it
        }
    }

    private val screenshotProvider = SimpleScreenshotProvider()
    private val selectedTab = mutableStateOf(Tab.ALL)

    private val emptyScreenshotText = selectedTab.map {
        when (it) {
            Tab.ALL -> "You have no screenshots"
            Tab.UPLOADED -> "No screenshots uploaded"
            Tab.LIKED -> "No favorite screenshots"
        }
    }

    private val searchBar by EssentialCollapsibleSearchbar()

    private val imageGroups: ListState<Triple<DateRange, Long, List<ScreenshotId>>> = stateBy {

        val dateMap = TreeMap<Long, Triple<DateRange, Long, MutableList<ScreenshotId>>>()

        for (value in DateRange.values()) {
            if (value == DateRange.MONTH_OTHER) {
                continue
            }

            val startTime = value.getStartTime()

            if (value == DateRange.EARLIER_MONTH && startTime > DateRange.LAST_WEEK.getStartTime()) {
                continue
            }

            dateMap[startTime] = Triple(value, startTime, mutableListOf())
        }

        val items = screenshotProvider.allPaths()
            .mapNotNull { screenshotProvider.propertyMap[it] }
            .map { properties -> properties to getImageTime(properties, true) }
            .sortedByDescending { (_, time) -> time }

        val otherMonthMap = mutableMapOf<Long, Triple<DateRange, Long, MutableList<ScreenshotId>>>()

        for (propertyTime in items) {
            val (property, imageTime) = propertyTime
            val entry = dateMap.floorEntry(imageTime.time)
            val group = if (entry != null) {
                entry.value
            } else {
                val date: Calendar = createDateOnlyCalendar(imageTime.time)
                date.set(Calendar.DAY_OF_MONTH, 1)

                otherMonthMap.getOrPut(date.time.time) {
                    Triple(
                        DateRange.MONTH_OTHER,
                        date.time.time,
                        mutableListOf()
                    )
                }
            }
            group.third.add(property.id)
        }

        val imageGroupList = (dateMap.values + otherMonthMap.values).toMutableList()

        imageGroupList.removeIf { it.third.isEmpty() }

        imageGroupList.sortWith(compareBy({
            -it.first.ordinal
        }, {
            -it.second
        }))

        imageGroupList
    }.toListState()

    private val filteredImageGroups: ListState<Triple<DateRange, Long, List<ScreenshotId>>> = stateBy {

        fun filter(metadata: ClientScreenshotMetadata?): Boolean {
            return when (selectedTab()) {
                Tab.ALL -> true
                Tab.UPLOADED -> metadata?.mediaId != null
                Tab.LIKED -> metadata?.favorite ?: false
            }
        }

        val searchText = searchBar.textContentV2()
        val selected = screenshotAttachmentManager.selectedImages()
        imageGroups().mapNotNull { (dateRange, startTime, images) ->
            Triple(dateRange, startTime, images.filter { id ->
                val props = screenshotProvider.propertyMap[id] ?: return@filter false
                filter(props.metadata) && props.matchesSearch(searchText) || id in selected
            }.takeUnless { it.isEmpty() } ?: return@mapNotNull null)
        }
    }.toListState()

    private var scrollComponent: ScrollComponent

    private val slider by ScreenshotItemsSlider(numberOfItemsPerRow, this)

    private val refreshHandler: Consumer<ScreenshotCollectionChangeEvent> = Consumer {
        screenshotProvider.reloadItems()
    }

    init {
        filteredImageGroups.onSetValueAndNow(this) { imageGroups ->
            screenshotProvider.currentPaths = imageGroups.flatMap { it.third }
        }

        val titleState = screenshotAttachmentManager.selectedImages
            .map { "Select Pictures - [${it.size}/10]" }
        val navigation: UIComponent
        val contentBox: UIComponent

        layout {
            column(Modifier.fillParent()) {
                box(Modifier.fillWidth().height(30f).color(EssentialPalette.COMPONENT_BACKGROUND)) {
                    row(Modifier.fillWidth(padding = 10f), Arrangement.SpaceBetween) {
                        text(
                            titleState.toV1(this@ScreenshotPicker),
                            modifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT)
                                .shadow(EssentialPalette.TEXT_SHADOW_LIGHT)
                        )
                        row(Arrangement.spacedBy(3f)) {
                            searchBar(Modifier.shadow())
                            screenshotAttachmentDoneButton(screenshotAttachmentManager)
                        }
                    }
                }
                box(Modifier.fillWidth().fillRemainingHeight()) {
                    contentBox = containerDontUseThisUnlessYouReallyHaveTo
                    column(Modifier.fillParent()) {
                        row(Modifier.fillWidth(padding = 10f).height(navigationMenuHeight), Arrangement.SpaceBetween) {
                            navigation = row(Arrangement.spacedBy(12f)) {
                                bind(selectedTab) { currentTab ->
                                    for (tab in Tab.values()) {
                                        text(
                                            tab.niceName,
                                            modifier = Modifier
                                                .color(if (currentTab == tab) EssentialPalette.ACCENT_BLUE else EssentialPalette.TEXT)
                                                .hoverColor(if (currentTab == tab) EssentialPalette.ACCENT_BLUE else EssentialPalette.TEXT_HIGHLIGHT)
                                                .hoverScope()
                                        ).onLeftClick {
                                            USound.playButtonPress()
                                            selectedTab.set(tab)
                                            focusCheck()
                                        }
                                    }
                                }
                            }
                            slider()
                        }
                        box(Modifier.fillWidth().fillRemainingHeight()) {
                            scrollComponent = scrollable(Modifier.fillParent(), vertical = true) {
                                column(Modifier.fillWidth()) {
                                    // Negative space to offset floating dates to top
                                    spacer(height = -navigationMenuHeight)
                                    forEach(filteredImageGroups) {
                                        screenshotDateGroup(
                                            it.first,
                                            it.second,
                                            numberOfItemsPerRow,
                                            it.third,
                                            screenshotProvider,
                                            screenshotAttachmentManager,
                                            navigation,
                                            contentBox
                                        )
                                    }
                                }
                            }
                            scrollGradient(scrollComponent, true, Modifier.height(30f), maxGradient = 153)
                            scrollGradient(scrollComponent, false, Modifier.height(30f), maxGradient = 153)
                            if_(filteredImageGroups.map { it.isEmpty() }) {
                                text(
                                    emptyScreenshotText.toV1(this@ScreenshotPicker),
                                    modifier = Modifier.alignVertical(Alignment.Start)
                                )
                            }
                        }
                    }
                    val scrollBar: UIComponent
                    floatingBox(Modifier.width(3f).fillHeight().alignHorizontal(Alignment.End(padding = -3f))) {
                        scrollBar = box(Modifier.fillWidth().color(EssentialPalette.SCROLLBAR))
                    }
                    scrollComponent.setVerticalScrollBarComponent(scrollBar, hideWhenUseless = true)
                }
            }
        }

        onLeftClick {
            focusCheck()
        }

        onKeyType { char, keyCode ->
            if (keyCode == UKeyboard.KEY_ENTER || keyCode == UKeyboard.KEY_ESCAPE) {
                screenshotAttachmentManager.isPickingScreenshots.set(false)
            } else {
                for (listener in Window.of(this).keyTypedListeners) {
                    listener(this, char, keyCode)
                }
            }
        }

        screenshotProvider.screenshotManager.registerScreenshotCollectionChangeHandler(refreshHandler)
        screenshotAttachmentManager.addCleanupHandler {
            screenshotProvider.cleanup()
        }
    }

    fun getScroller(): ScrollComponent {
        return scrollComponent
    }

    override fun draw(matrixStack: UMatrixStack) {
        screenshotProvider.frameUpdate(scrollComponent)
        super.draw(matrixStack)
    }

    fun focusCheck() {
        if (screenshotAttachmentManager.isPickingScreenshots.get()) {
            grabWindowFocus()
        }
    }

    private enum class Tab(val niceName: String) {
        ALL("All"),
        LIKED("Favorites"),
        UPLOADED("Uploads"),
    }

    companion object {
        val SCREENSHOT_PADDING = 3f
        val SCREENSHOT_SIDE_PADDING = 7f
    }

}
