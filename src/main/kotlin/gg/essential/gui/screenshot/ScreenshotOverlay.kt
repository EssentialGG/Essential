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
package gg.essential.gui.screenshot

import gg.essential.Essential
import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.data.OnboardingData
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.invisible
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ImageLoadCallback
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.common.or
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.modals.NotAuthenticatedModal
import gg.essential.gui.modals.TOSModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.overlay.LayerPriority
import gg.essential.gui.screenshot.ScreenshotOverlay.animating
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.screenshot.components.createShareScreenshotModal
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.onAnimationFrame
import gg.essential.universal.UResolution
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.io.File
import java.util.function.Consumer

object ScreenshotOverlay {
    private val layer = GuiUtil.createPersistentLayer(LayerPriority.Notifications)
    private val window: Window = layer.window
    var animating = true

    fun push(file: File) {
        pushToast(ScreenshotPreviewToast(file))
    }

    fun pushUpload(): Consumer<ScreenshotUploadToast.ToastProgress> {
        val toast = ScreenshotUploadToast()

        Notifications.push("", "") {
            timerEnabled = toast.timerEnabled
            withCustomComponent(Slot.LARGE_PREVIEW, toast)
        }
        return toast.createProgressConsumer()
    }

    private fun pushToast(toast: ScreenshotToast) {
        // Drop new screenshots if too many are taken in a short period
        if (window.children.size > 3) {
            return
        }
        toast childOf window

        if (window.children.size > 3) {
            clearScreenshot(window.children.first() as ScreenshotToast)
        }
    }

    fun clearScreenshot(screenshotToast: ScreenshotToast) {
        val index = window.children.indexOf(screenshotToast)
        screenshotToast.animateAway {
            window.childrenOfType(ScreenshotToast::class.java).forEach {
                if (index < window.children.indexOf(it)) {
                    it.animateUp()
                }
            }
        }
    }

    fun pauseAll() {
        animating = false
    }

    fun resumeAll() {
        animating = true
    }

    fun hide() {
        layer.rendered = false
    }

    fun show() {
        layer.rendered = true
    }

    fun hasActiveNotifications(): Boolean {
        return window.children.size > 0
    }

    /**
     * Called when a file is deleted by the user so that the
     * toast can be removed from the UI.
     */
    fun delete(file: File) {
        val component = window.children.firstOrNull {
            it is ScreenshotPreviewToast && it.file.name == file.name
        } ?: return
        clearScreenshot(component as ScreenshotToast)
    }
}

private const val TOP_PADDING = 5f
private const val RIGHT_PADDING = 5f

open class ScreenshotToast : UIContainer() {
    private var animatingAway = false

    init {
        constrain {
            width = 10.percentOfWindow * 2
            x = RIGHT_PADDING.pixels(alignOpposite = true)
            y = max(SiblingConstraint(TOP_PADDING), TOP_PADDING.pixels)
        }
    }

    fun animateAway(callback: () -> Unit) {
        if (animatingAway) {
            return
        }
        animatingAway = true

        val targetConstraint: XConstraint = 100.percent

        // Elementa will never execute the `onComplete` block of an animation if
        // another animation is started before the first finishes. However, the
        // animation will visually reach 100% and stay there. Therefore, we
        // use this work around to ensure that the callback is executed and
        // this component is removed from the window's children when the animation
        // reaches 100%.
        //
        // This issue becomes apparent if the screenshot above this one finishes
        // animating away while this one is animating away because both screenshots
        // were taken in quick succession.
        onAnimationFrame {
            if (getLeft() == targetConstraint.getXPosition(this)) {
                callback()
                Window.of(this@ScreenshotToast).removeChild(this@ScreenshotToast)
            }
        }

        animate {
            setXAnimation(Animations.IN_EXP, 0.5f, targetConstraint)
        }
    }


    fun animateUp() {
        // Once the previous sibling is removed from the component tree, SiblingConstraint
        // will instantly update the Y value it returns. As a result, the "start" value
        // that is interpolated between changes and the animation doesn't work as desired.
        // Therefore, we fix the Y starting position wherever the component is now to use that
        // as the starting location for the animation.
        setY(getTop().pixels)

        animate {
            setYAnimation(Animations.OUT_EXP, 0.5f, max(SiblingConstraint(TOP_PADDING), TOP_PADDING.pixels))
        }
    }

}

class ScreenshotPreviewToast(val file: File) : ScreenshotToast() {

    private val screenshotId = LocalScreenshot(file.toPath())
    private val aspectRatio = BasicState(UResolution.scaledWidth / UResolution.scaledHeight.toFloat())
    private var animationFramesRemaining = -1
    private val hovered = hoveredState()
    private val favoriteState = BasicState(false)
    private val favoriteIcon = favoriteState.map {
        if (it) {
            EssentialPalette.HEART_FILLED_9X
        } else {
            EssentialPalette.HEART_EMPTY_9X
        }
    }
    private val favoriteTooltip = favoriteState.map {
        if (it) {
            "Remove Favorite"
        } else {
            "Favorite"
        }
    }
    private val background by UIBlock(Color.WHITE).constrain {
        width = AspectPreservingFillConstraint(aspectRatio)
        height = 100.percent
    } childOf this

    private val image by UIImage.ofFile(file).constrain {
        color = Color.WHITE.invisible().toConstraint()
        width = AspectPreservingFillConstraint(aspectRatio)
        height = 100.percent
    } childOf this

    private val hoverComponent by UIBlock(Color(0, 0, 0, 100)).constrain {
        width = 100.percent
        height = 100.percent
    }.bindParent(this, hovered)

    init {
        constrain {
            height = ChildBasedSizeConstraint()
        }

        val topLeft by getManageActionForSlot(ScreenshotPreviewActionSlot.TOP_LEFT)
        val topRight by getManageActionForSlot(ScreenshotPreviewActionSlot.TOP_RIGTH)
        val bottomLeft by getManageActionForSlot(ScreenshotPreviewActionSlot.BOTTOM_LEFT)
        val bottomRight by getManageActionForSlot(ScreenshotPreviewActionSlot.BOTTOM_RIGHT)

        val halfHeightModifier = Modifier.fillWidth().fillHeight(0.5f)
        val halfWidthModifier = Modifier.fillWidth(0.5f).fillHeight()

        hoverComponent.layoutAsColumn {
            row(halfHeightModifier) {
                topLeft(halfWidthModifier)
                topRight(halfWidthModifier)
            }
            row(halfHeightModifier) {
                bottomLeft(halfWidthModifier)
                bottomRight(halfWidthModifier)
            }
        }

        aspectRatio.onSetValueAndNow {
            setHeight(AspectConstraint(1 / it))
        }

        favoriteState.onSetValue {
            Essential.getInstance().connectionManager.screenshotManager.setFavorite(file.toPath(), it)
        }

        hovered.onSetValue {
            if (it) {
                ScreenshotOverlay.pauseAll()
            } else {
                ScreenshotOverlay.resumeAll()
            }
        }
        enableEffect(OutlineEffect(EssentialPalette.TEXT, 1f))
    }

    override fun afterInitialization() {
        super.afterInitialization()
        // Delayed because there is a race condition between the class initializing and the image loading.
        // If the image loads first, then the animate call inside the block will fail because this component
        // is not yet part of a widow.
        image.supply(ImageLoadCallback {
            aspectRatio.set(this.width / this.height.toFloat())
            image.animate {
                setColorAnimation(Animations.LINEAR, 0.5f, Color.WHITE.toConstraint())
            }
            val time =
                when (EssentialConfig.screenshotToastDuration) {
                    1 -> 5
                    2 -> 7
                    else -> 3
                }
            animationFramesRemaining = Window.of(this@ScreenshotPreviewToast).animationFPS * time
        })
    }

    private fun getManageActionForSlot(slot: ScreenshotPreviewActionSlot): UIComponent {
        return when (slot.action) {
            ScreenshotPreviewAction.COPY_PICTURE -> {
                ManageAction("Copy Picture", EssentialPalette.COPY_9X).onLeftClick {
                    Multithreading.runAsync {
                        Essential.getInstance().connectionManager.screenshotManager.copyScreenshotToClipboard(file)
                    }
                }
            }

            ScreenshotPreviewAction.COPY_LINK -> {
                ManageAction("Copy Link", EssentialPalette.LINK_8X7).onLeftClick {
                    clear()

                    val connectionManager = Essential.getInstance().connectionManager

                    val upload: () -> Unit = { connectionManager.screenshotManager.uploadAndCopyLinkToClipboard(file.toPath()) }

                    if (!OnboardingData.hasAcceptedTos()) {
                        GuiUtil.pushModal { manager -> 
                            TOSModal(
                                manager,
                                unprompted = false,
                                requiresAuth = true,
                                confirmAction = { upload() },
                                cancelAction = {},
                            )
                        }
                    } else if (!connectionManager.isAuthenticated) {
                        GuiUtil.pushModal { NotAuthenticatedModal(it) { upload() } }
                    } else {
                        upload()
                    }
                }
            }

            ScreenshotPreviewAction.FAVORITE -> {
                ManageAction(favoriteTooltip, favoriteIcon).apply {
                    imageColor.rebind((hovered or favoriteState).map { if (it) EssentialPalette.TEXT_RED else EssentialPalette.TEXT })
                }.onLeftClick {
                    favoriteState.set { !it }
                }
            }

            ScreenshotPreviewAction.DELETE -> {
                ManageAction("Delete", EssentialPalette.TRASH_9X).apply {
                    imageColor.rebind(hovered.map { if (it) EssentialPalette.TEXT_RED else EssentialPalette.TEXT })
                }.onLeftClick {
                    Essential.getInstance().connectionManager.screenshotManager.handleDelete(file, false)

                    val screen = GuiUtil.openedScreen()
                    if (screen is ScreenshotBrowser) {
                        screen.externalDelete(setOf(file.toPath()))
                    }
                }
            }

            ScreenshotPreviewAction.SHARE -> {
                ManageAction("Send to Friends", EssentialPalette.SOCIAL_10X).onLeftClick {
                    GuiUtil.pushModal { createShareScreenshotModal(it, screenshotId) }
                }
            }

            ScreenshotPreviewAction.EDIT -> {
                ManageAction("Edit", EssentialPalette.EDIT_10X7).onLeftClick {
                    clear()
                    GuiUtil.openScreen { ScreenshotBrowser(file.toPath()) }
                }
            }
        }
    }

    inner class ManageAction(
        tooltip: State<String>,
        icon: State<ImageFactory>,
    ) : UIBlock() {

        constructor(tooltip: String, icon: ImageFactory) : this(
            BasicState(tooltip),
            BasicState(icon)
        )

        val hovered = hoveredState()

        private val unscaled by ShadowIcon(icon, BasicState(true))

        private val imageContainer by UIContainer().centered().constrain {
            width = basicWidthConstraint { it.getHeight() / unscaled.getHeight() * unscaled.getWidth() }
            // For purposes of calculating scale factor, the image icon is assumed to be
            // 7 pixels tall. That way, all icons will have the same scale
            height = 25.percent / 7.pixels * basicHeightConstraint {
                unscaled.getHeight().coerceAtLeast(1f)
            }
        } childOf this

        val imageColor = EssentialPalette.getTextColor(hovered).map { it }

        init {

            bindEssentialTooltip(hoveredState(), tooltip, windowPadding = RIGHT_PADDING)

            setColor(hovered.map {
                if (it) {
                    Color(255, 255, 255, 50)
                } else {
                    Color(0, 0, 0, 0)
                }
            }.toConstraint())

            onLeftClick {
                USound.playButtonPress()
            }
            icon.onSetValueAndNow { factory ->
                imageContainer.clearChildren()
                // Main image
                val image by factory.create().constrain {
                    width = 100.percent
                    height = 100.percent
                    color = imageColor.toConstraint()
                }  // Parent defined after shadow to avoid shadow being drawn on top of image

                // Shadow
                factory.create().constrain {
                    width = 100.percent
                    height = 100.percent
                    x = basicXConstraint {
                        image.getLeft() + image.getWidth() / unscaled.getWidth()
                    }
                    y = basicYConstraint {
                        image.getTop() + image.getHeight() / unscaled.getHeight()
                    }
                    color = EssentialPalette.TEXT_SHADOW.toConstraint()
                } childOf imageContainer

                image childOf imageContainer
            }
        }
    }

    private fun clear() {
        ScreenshotOverlay.clearScreenshot(this)
    }

    override fun animationFrame() {
        if (animating && animationFramesRemaining > 0) {
            animationFramesRemaining--
            if (animationFramesRemaining == 0) {
                clear()
            }
        }
        super.animationFrame()
    }

}

enum class ScreenshotPreviewActionSlot(val defaultAction: ScreenshotPreviewAction) {

    TOP_LEFT(ScreenshotPreviewAction.EDIT),
    TOP_RIGTH(ScreenshotPreviewAction.FAVORITE),
    BOTTOM_LEFT(ScreenshotPreviewAction.COPY_PICTURE),
    BOTTOM_RIGHT(ScreenshotPreviewAction.SHARE),
    ;

    val action: ScreenshotPreviewAction
        get() {
            return ScreenshotPreviewAction.values().getOrNull(
                    when (this) {
                        TOP_LEFT -> EssentialConfig.screenshotOverlayTopLeftAction
                        TOP_RIGTH -> EssentialConfig.screenshotOverlayTopRightAction
                        BOTTOM_LEFT -> EssentialConfig.screenshotOverlayBottomLeftAction
                        BOTTOM_RIGHT -> EssentialConfig.screenshotOverlayBottomRightAction
                    }
                ) ?: defaultAction
        }

}

// The order of these actions is used/replicated in the settings.
// Remember to change in both places at once
enum class ScreenshotPreviewAction(val displayName: String) {

    COPY_PICTURE("Copy Picture"),
    COPY_LINK("Copy Link"),
    FAVORITE("Favorite"),
    DELETE("Delete"),
    SHARE("Share to Friends"),
    EDIT("Edit"),
    ;

}

class ScreenshotUploadToast : UIContainer() {

    private val maxCompletionDelayMillis = 500
    private val startUploadMillis = System.currentTimeMillis()
    private val initialProgress: ToastProgress = ToastProgress.Step(0)
    private var targetProgress: ToastProgress = initialProgress
    private var currentProgress: ToastProgress = targetProgress
    val timerEnabled = BasicState(false)
    private val stateText by UIText("Uploading...").constrain {
        x = SiblingConstraint(6f)
        y = CenterConstraint()
        color = EssentialPalette.TEXT_HIGHLIGHT.toConstraint()
    } childOf this

    private val progressContainer by UIContainer().constrain {
        x = SiblingConstraint(4f)
        y = CenterConstraint()
        width = FillConstraint(useSiblings = false) - 1.pixel
        height = 9.pixels
    } childOf this effect OutlineEffect(EssentialPalette.TEXT_HIGHLIGHT, 1f)

    private val progressBlock by UIBlock(EssentialPalette.TEXT_HIGHLIGHT).constrain {
        width = 0.pixels
        height = 100.percent
    } childOf progressContainer

    init {
        constrain {
            width = 100.percent
            height =
                ChildBasedMaxSizeConstraint() + 2.pixels // so that the outline is not scissored out of existence by the notification
        }
    }

    override fun animationFrame() {
        super.animationFrame()
        updateProgress()
    }

    private fun updateProgress() {
        val targetProgress = targetProgress
        if (currentProgress != targetProgress) {
            val previousProgress = currentProgress
            currentProgress = targetProgress

            // If we went straight to complete, skip the upload bar and just show the result
            if (targetProgress is ToastProgress.Complete && previousProgress == initialProgress) {
                fireComplete(targetProgress)
                return
            }

            val targetPercent = if (targetProgress is ToastProgress.Step) {
                targetProgress.completionPercent
            } else {
                100
            }
            progressBlock.animate {
                setWidthAnimation(Animations.LINEAR, 0.5f, targetPercent.pixels)
                onComplete {
                    if (targetProgress is ToastProgress.Complete) {
                        // If we were successful, and it's been under maxCompletionDelayMillis, use some delay for dramatic effect.
                        val timeElapsedMillis = System.currentTimeMillis() - startUploadMillis
                        val delayMillis = maxCompletionDelayMillis - timeElapsedMillis
                        if (targetProgress.success && delayMillis > 0) {
                            delay(delayMillis) { fireComplete(targetProgress) }
                        } else {
                            fireComplete(targetProgress)
                        }
                    }
                }
            }
        }
    }

    fun createProgressConsumer(): Consumer<ToastProgress> {
        return Consumer<ToastProgress> { t ->
            Window.enqueueRenderOperation {
                targetProgress = t
            }
        }
    }

    private fun fireComplete(status: ToastProgress.Complete) {
        val action = {
            timerEnabled.set(true)
            removeChild(progressContainer)
            stateText.setText(status.message)
            this.insertChildAt(
                ShadowIcon(
                    if (status.success) {
                        EssentialPalette.CHECKMARK_7X5
                    } else {
                        EssentialPalette.CANCEL_5X
                    }, true
                ).rebindPrimaryColor(BasicState(EssentialPalette.TEXT_HIGHLIGHT))
                    .rebindShadowColor(BasicState(EssentialPalette.MODAL_OUTLINE)).constrain {
                        y = CenterConstraint()
                    }, 0
            )
            USound.playLevelupSound()
        }
        action()
    }

    sealed class ToastProgress {

        data class Complete(val message: String, val success: Boolean) : ToastProgress()

        data class Step(val completionPercent: Int) : ToastProgress()
    }
}
