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

import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.add
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.remove
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.BasicHeightModifier
import gg.essential.gui.layoutdsl.BasicWidthModifier
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignBoth
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.fillParent
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.icon
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.width
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.components.invalidScreenshotView
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.universal.USound
import gg.essential.vigilance.utils.onLeftClick

class SelectableScreenshotPreview(
    screenshotId: ScreenshotId,
    simpleScreenshotProvider: SimpleScreenshotProvider,
    private val screenshotAttachmentManager: ScreenshotAttachmentManager,
) : ScreenshotPreview(
    screenshotId,
    simpleScreenshotProvider,
) {

    private val selected = screenshotAttachmentManager.selectedImages.map { it.contains(screenshotId) }

    private val errored = imgTexture.map { it?.error ?: false }

    init {
        this.layout {
            box(Modifier.fillParent()) {
                box(
                    Modifier.hoverColor(EssentialPalette.ACCENT_BLUE).hoverScope()
                        .then(BasicWidthModifier { AspectPreservingFillConstraint(imageAspectState.toV1(this@SelectableScreenshotPreview)) })
                        .then(BasicHeightModifier { AspectPreservingFillConstraint(imageAspectState.toV1(this@SelectableScreenshotPreview)) })
                ) {
                    if_(errored) {
                        invalidScreenshotView(Modifier.fillParent(padding = 2f))
                    } `else` {
                        img(Modifier.fillParent(padding = 2f))
                    }


                    if_(selected) {
                        box(Modifier.width(17f).height(15f).alignBoth(Alignment.End)) {
                            icon(EssentialPalette.CHECKMARK_7X5, Modifier.color(EssentialPalette.ACCENT_BLUE))
                        }
                    }
                }
            }
        }

        onLeftClick {
            if (errored.get()) {
                return@onLeftClick
            }

            setSelected(!selected.get())
        }
    }

    private fun setSelected(selected: Boolean) {
        if (selected) {
            if (screenshotAttachmentManager.selectedImages.get().size + 1 > screenshotAttachmentManager.maxSelectAmount) {
                return
            }
            screenshotAttachmentManager.selectedImages.add(screenshotId)
        } else {
            screenshotAttachmentManager.selectedImages.remove(screenshotId)
        }
        USound.playButtonPress()
    }

}