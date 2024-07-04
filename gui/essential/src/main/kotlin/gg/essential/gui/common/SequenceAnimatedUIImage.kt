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
package gg.essential.gui.common

import gg.essential.config.LoadsResources
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.constraints.CopyConstraintColor
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.state.BasicState
import java.util.concurrent.TimeUnit

class SequenceAnimatedUIImage @LoadsResources("%pathPrefix%[0-9]+%pathSuffix%") constructor(
    val pathPrefix: String,
    val pathSuffix: String,
    val frameCount: Int,
    val delay: Long,
    val timeUnit: TimeUnit,
) : UIContainer() {

    val currentFrame = BasicState(0)
    val frameDelay = BasicState(1L)
    private var activeTimer = -1

    var currentFrameComponent: UIImage? = null
        private set

    init {
        val frames = Array(frameCount, init = {
            UIImage.ofResourceCached("$pathPrefix${it + 1}$pathSuffix").constrain {
                width = 100.percent
                height = 100.percent
                color = CopyConstraintColor() boundTo this@SequenceAnimatedUIImage
            }.apply {
                supply(AutoImageSize(this@SequenceAnimatedUIImage))
            }
        })
        currentFrame.onSetValueAndNow {
            clearChildren()
            addChild(frames[it])
            currentFrameComponent = frames[it]
        }
        frameDelay.onSetValue {
            if (activeTimer != -1)
                stopTimer(activeTimer)
            activeTimer = startTimer(it, 0) {
                currentFrame.set { current ->
                    (1 + current + frameCount) % frameCount
                }
            }
        }
        frameDelay.set(timeUnit.toMillis(delay))
    }
}