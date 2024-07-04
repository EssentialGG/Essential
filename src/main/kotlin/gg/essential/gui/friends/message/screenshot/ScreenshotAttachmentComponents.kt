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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.coerceIn
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.BasicYModifier
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignHorizontal
import gg.essential.gui.layoutdsl.animateWidth
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.childBasedMaxHeight
import gg.essential.gui.layoutdsl.childBasedWidth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.effect
import gg.essential.gui.layoutdsl.fillHeight
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.floatingBox
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.screenshot.DateRange
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.universal.USound
import gg.essential.gui.util.onAnimationFrame
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color

fun LayoutScope.screenshotAttachmentUploadBox(
    screenshotAttachmentManager: ScreenshotAttachmentManager
) {
    row(Modifier.childBasedMaxHeight(9f).color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)) {
        spacer(width = 10f)
        icon(EssentialPalette.PICTURES_10X10, Modifier.color(EssentialPalette.TEXT))
        spacer(width = 6f)
        text("Uploading...", Modifier.color(EssentialPalette.TEXT).shadow(Color.BLACK))
        spacer(width = 11f)
        box(Modifier.width(100f).height(3f).color(EssentialPalette.GUI_BACKGROUND).shadow(Color.BLACK)) {
            box(
                Modifier.animateWidth(
                    screenshotAttachmentManager.totalProgressPercentage.map { { it.pixels } },
                    0.5f,
                    Animations.LINEAR
                ).fillHeight().color(EssentialPalette.TOAST_PROGRESS).alignHorizontal(Alignment.Start)
            )
        }
        spacer(width = 10f)
    }.onAnimationFrame {
        screenshotAttachmentManager.updateProgress()
    }
}

fun LayoutScope.screenshotAttachmentDoneButton(screenshotAttachmentManager: ScreenshotAttachmentManager) {
    box(
        Modifier.width(43f).height(17f)
            .color(MenuButton.BLUE.buttonColor)
            .hoverColor(MenuButton.LIGHT_BLUE.buttonColor)
            .shadow()
            .hoverScope()
    ) {
        text(
            "Done",
            Modifier.shadow(),
            centeringContainsShadow = false
        )
    }.onLeftClick {
        USound.playButtonPress()
        screenshotAttachmentManager.isPickingScreenshots.set(false)
    }
}

fun LayoutScope.screenshotDateGroup(
    range: DateRange,
    startTime: Long,
    numberOfItemsPerRow: State<Int>,
    screenshotIds: List<ScreenshotId>,
    screenshotProvider: SimpleScreenshotProvider,
    screenshotAttachmentManager: ScreenshotAttachmentManager,
    navigation: UIComponent,
    contentBox: UIComponent
) {
    val divider: UIComponent
    val content: UIComponent

    box(Modifier.fillWidth().childBasedMaxHeight()) {
        content = containerDontUseThisUnlessYouReallyHaveTo
        column(Modifier.fillWidth().childBasedHeight()) {
            divider = box(Modifier.fillWidth().childBasedMaxHeight(11f)) {
                box(
                    Modifier.fillWidth(padding = ScreenshotPicker.SCREENSHOT_SIDE_PADDING + 2f).height(1f)
                        .color(EssentialPalette.COMPONENT_BACKGROUND)
                )
                box(Modifier.childBasedWidth(4f).childBasedHeight().color(EssentialPalette.GUI_BACKGROUND)) {
                    // Blank text to measure correct size for background of real text
                    text(
                        range.getName(startTime),
                        modifier = Modifier.color(EssentialPalette.GUI_BACKGROUND)
                            .shadow(EssentialPalette.GUI_BACKGROUND)
                    )
                }
            }
            column(
                Modifier.fillWidth().childBasedHeight(),
                Arrangement.spacedBy(ScreenshotPicker.SCREENSHOT_PADDING)
            ) {
                bind(numberOfItemsPerRow) { itemsPerRows ->
                    for (list in screenshotIds.chunked(itemsPerRows)) {
                        row(
                            Modifier.fillWidth(padding = ScreenshotPicker.SCREENSHOT_SIDE_PADDING),
                            Arrangement.equalWeight(ScreenshotPicker.SCREENSHOT_PADDING)
                        ) {
                            val imageModifier = Modifier.heightAspect(9 / 16f)
                            for (i in 0 until itemsPerRows) {
                                if (i < list.size) {
                                    SelectableScreenshotPreview(
                                        list[i],
                                        screenshotProvider,
                                        screenshotAttachmentManager
                                    )(imageModifier)
                                } else {
                                    box(imageModifier)
                                }
                            }
                        }
                    }
                }
            }
        }
        floatingBox(
            Modifier.childBasedWidth(4f)
                .effect { ScissorEffect(contentBox) }
                .then(BasicYModifier {
                    // Position the title in the center of navigation
                    (CenterConstraint() boundTo navigation)
                        // but force it to stay within the content's bounds, so the titles of different groups never overlap
                        .coerceIn(
                            CenterConstraint() boundTo divider,
                            0.pixels(alignOpposite = true) boundTo content
                        )
                })
        ) {
            text(range.getName(startTime), centeringContainsShadow = true)
        }
    }
}