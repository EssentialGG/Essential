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
package gg.essential.gui.screenshot.editor

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.UINanoVG
import gg.essential.gui.common.or
import gg.essential.gui.common.weak
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.screenshot.editor.change.Change
import gg.essential.gui.screenshot.editor.change.CropChange
import gg.essential.gui.screenshot.editor.change.VectorStroke
import gg.essential.gui.screenshot.image.PixelBufferTexture
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.network.connectionmanager.media.ScreenshotManager
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.util.Multithreading
import gg.essential.util.animateColor
import gg.essential.util.lwjgl3.api.nanovg.NanoVG
import gg.essential.vigilance.gui.VigilancePalette
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

/**
 * Can be improved by abstracting cropping functions to a cropping [Tool] class
 */
class ScreenshotCanvas(val screenshot: State<PixelBufferTexture?>) : UIContainer() {
    var onDraw: UIContainer.(Float, Float, Int) -> Unit = { _, _, _ -> }

    var mouseButton = -1

    private val padding = 2

    // screenshot [UIImage] which is currently being edited
    val screenshotDisplay = object : UIContainer() {

        override fun mouseMove(window: Window) {
            if (mouseButton != -1) {
                val (x, y) = getMousePosition()
                onDraw(x, y, mouseButton)
            }

            super.mouseMove(window)
        }

        override fun draw(matrixStack: UMatrixStack) {
            vectorEditingOverlay.draw(matrixStack)
            super.draw(matrixStack)
        }

    }.onMouseClick { event ->
        mouseButton = event.mouseButton
        event.stopPropagation()
    }.onMouseRelease {
        mouseButton = -1
    }.constrain {
        height = 100.percent() - (padding * 2).pixels()
        width = 100.percent() - (padding * 2).pixels()
        x = padding.pixels()
        y = padding.pixels()
    } childOf this


    val background = UIBlock(VigilancePalette.getModalBackground()).constrain {
        height = 100.percent()
        width = 100.percent()
    } childOf screenshotDisplay

    val cropSettings: Crop = Crop()

    val retainedImage by UIContainer().constrain {
        x = RelativeConstraint(1f).bindValue(cropSettings.left)
        y = RelativeConstraint(1f).bindValue(cropSettings.top)
        width = RelativeConstraint(1f).bindValue(cropSettings.right.zip(cropSettings.left).map { it.first - it.second })
        height =
            RelativeConstraint(1f).bindValue(cropSettings.bottom.zip(cropSettings.top).map { it.first - it.second })
    } childOf screenshotDisplay effect ScissorEffect()


    // anything drawn is displayed via this component
    // tools such as [PenTool] mutate its image and update it
    val vectorEditingOverlay: VectorEditingOverlay = VectorEditingOverlay(screenshot).constrain {
        x = 0.pixels boundTo screenshotDisplay
        y = 0.pixels boundTo screenshotDisplay
        height = 100.percent() boundTo screenshotDisplay
        width = 100.percent() boundTo screenshotDisplay
    } childOf retainedImage


    val undoEnabled by vectorEditingOverlay::undoEnabled
    val redoEnabled by vectorEditingOverlay::redoEnabled

    init {
        CropAlignment.values().forEach { alignment ->
            ImageCropItem(alignment) childOf this
        }
    }


    /**
     * NanoVG based editing overlay which handles drawing all edits as well as drawing the parts of the screenshot retained from cropping. [UINanoVG]
     */
    open inner class VectorEditingOverlay(val image: State<PixelBufferTexture?>) : UINanoVG() {
        private val history: Stack<Change> = Stack()
        private val future: Stack<Change> = Stack()
        private val screenshotImage = ScreenshotImage(image)
        var scale = 1f

        val undoEnabled = BasicState(false)
        val redoEnabled = BasicState(false)

        private val weakImage = image.weak()
        init {
            weakImage.onSetValue { markDirty() }
        }

        /**
         * Resets the state of the editor back to default
         */
        fun reset() {
            undoEnabled.set(false)
            redoEnabled.set(false)
            history.clear()
            future.clear()
            markDirty()
        }

        constructor(veo: VectorEditingOverlay) : this(veo.image) {
            this.history.addAll(veo.history)
            this.future.addAll(veo.future)
            undoEnabled.set(veo.undoEnabled.get())
            redoEnabled.set(veo.redoEnabled.get())
        }

        override fun render(matrixStack: UMatrixStack, width: Float, height: Float) {
            renderImage(matrixStack, width, height)
            super.render(matrixStack, width, height)
        }

        override fun renderVG(matrixStack: UMatrixStack, vg: NanoVG, width: Float, height: Float) {
            history.filterIsInstance<VectorStroke>().forEach { vs ->
                vs.render(vg, width, height, scale)
            }
        }

        open fun renderImage(matrixStack: UMatrixStack, width: Float, height: Float) {
            screenshotImage.renderImage(matrixStack, Color.WHITE, width.toDouble(), height.toDouble())
        }

        /**
         * Adds a new change to the history stack
         *
         * This removes all "future" changes!
         * @param change Change to add
         * @return this
         */
        fun pushChange(change: Change) = apply {
            future.clear()
            history.push(change)
            markDirty()
            undoEnabled.set(true)
            redoEnabled.set(false)
        }

        /**
         * Tries to undo the last stroke
         * @return this
         */
        fun undo() = apply {
            if (history.empty()) return@apply
            redoEnabled.set(true)
            future.push(history.pop().also { it.undo(this@ScreenshotCanvas) })
            undoEnabled.set(!history.isEmpty())
        }

        /**
         * Tries to redo the "next" stroke
         * @return this
         */
        fun redo() = apply {
            if (future.empty()) return@apply
            history.push(future.pop().also { it.redo(this@ScreenshotCanvas) })
            undoEnabled.set(true)
            redoEnabled.set(!future.isEmpty())
        }
    }

    /**
     * Returns a state that is true if and only if the user has made any changes
     * to the screenshot currently being edited. Changes can include adjusting
     * crop settings or drawing.
     */
    fun getHasChanges(): State<Boolean> {
        return undoEnabled
    }

    /**
     * Exports the screenshot currently being edited to a file
     * If [temp] is true, the output is a temporary file.
     * Otherwise, the output is stored inside the screenshot folder
     * and added to the view
     */
    fun exportImage(
        source: ScreenshotId,
        screenshotManager: ScreenshotManager,
        screenshotBrowser: ScreenshotBrowser,
        temp: Boolean,
        favorite: Boolean = false,
        viewAfter: Boolean,
    ): CompletableFuture<File> {

        // Cloned because the values are accessed on another thread and may have been reset
        val cropSettings = cropSettings.clone()

        val completableFuture = CompletableFuture<File>()

        val screenshot = source.open().use {
            try {
                ImageIO.read(it) ?: throw IOException("Failed to load original image from $source")
            } catch (e: Exception) {
                completableFuture.completeExceptionally(e)
                return completableFuture
            }
        }

        val fullWidth = screenshot.width
        val fullHeight = screenshot.height
        val drawableWidth = screenshotDisplay.getWidth().toInt() * UResolution.scaleFactor.toInt()

        val buffer = BufferUtils.createFloatBuffer(fullWidth * fullHeight * 4)
        val veoCopy = object : VectorEditingOverlay(vectorEditingOverlay) {
            override fun render(matrixStack: UMatrixStack, width: Float, height: Float) {
                super.render(matrixStack, width, height)

                GL11.glReadPixels(
                    0,
                    0,
                    width.toInt(),
                    height.toInt(),
                    GL11.GL_RGBA,
                    GL11.GL_FLOAT,
                    buffer
                )
            }

            // We don't want to render the original screenshot when exporting.
            override fun renderImage(matrixStack: UMatrixStack, width: Float, height: Float) {
            }
        }
        veoCopy.scale = fullWidth / drawableWidth.toFloat()
        veoCopy.drawFrameBuffer(fullWidth / UResolution.scaleFactor, fullHeight / UResolution.scaleFactor)
        veoCopy.delete()
        // Fork as soon as we can to avoid freezing the main thread
        Multithreading.runAsync {
            buffer.rewind()
            val imgData = (0 until buffer.limit()).map { i ->
                (buffer.get(i) * 255f).toInt()
            }.chunked(fullWidth * 4) { chunk ->
                chunk.chunked(4) { list ->
                    (((list[3] and 0xFF) shl 24) or
                        ((list[0] and 0xFF) shl 16) or
                        ((list[1] and 0xFF) shl 8) or
                        (list[2] and 0xFF))
                }
            }.reversed().flatten().toIntArray()
            val image = BufferedImage(
                fullWidth,
                fullHeight,
                BufferedImage.TYPE_INT_ARGB
            )
            image.setRGB(0, 0, fullWidth, fullHeight, imgData, 0, fullWidth)
            screenshot.graphics.drawImage(image, 0, 0, null)


            val left = (fullWidth * cropSettings.left.get()).toInt()
            val right = (fullWidth * cropSettings.right.get()).toInt()
            val top = (fullHeight * cropSettings.top.get()).toInt()
            val bottom = (fullHeight * cropSettings.bottom.get()).toInt()

            val croppedImage = screenshot.getSubimage(left, top, right - left, bottom - top)
            if (temp) {
                val tempFile = File.createTempFile("screenshot", null)
                ImageIO.write(croppedImage, "png", tempFile)
                completableFuture.complete(tempFile)
            } else {
                completableFuture.complete(
                    screenshotManager.handleScreenshotEdited(
                        source,
                        when (source) {
                            is LocalScreenshot -> screenshotManager.screenshotMetadataManager.getOrCreateMetadata(source.path)
                            is RemoteScreenshot -> ClientScreenshotMetadata(source.media)
                        },
                        croppedImage,
                        screenshotBrowser,
                        favorite,
                        viewAfter
                    )
                )
            }
        }

        return completableFuture
    }

    inner class ImageCropItem(val alignment: CropAlignment) : UIContainer() {

        var dragging = false

        //Accounts for position inside the element that we are dragging from
        var xDragOffset = 0f
        var yDragOffset = 0f

        init {
            constrain {
                height = 15.pixels()
                width = 15.pixels()
                x = if (alignment.centerX) {
                    CenterConstraint()
                } else {
                    (-padding).pixels(alignOpposite = alignment.alignOpX)
                } boundTo retainedImage
                y = if (alignment.centerY) {
                    CenterConstraint()
                } else {
                    (-padding).pixels(alignOpposite = alignment.alignOpY)
                } boundTo retainedImage
            }

            if (alignment.corner) {
                addChild {
                    UIBlock(EssentialPalette.TEXT).constrain {
                        x = (if (alignment.alignOpX) 0 else 0).pixels(alignOpposite = false, alignOutside = false)
                        y = 0.pixels(alignOpposite = alignment.alignOpY, alignOutside = false)
                        width = 15.pixels()
                        height = 2.pixels()
                    }
                }
                addChild {
                    UIBlock(EssentialPalette.TEXT).constrain {
                        x = 0.pixels(alignOpposite = alignment.alignOpX, alignOutside = false)
                        y = (if (alignment.alignOpY) 0 else 0).pixels(alignOpposite = false, alignOutside = false)
                        width = 2.pixels()
                        height = 15.pixels()
                    }
                }
            } else {
                addChild {
                    UIBlock(EssentialPalette.TEXT).constrain {
                        x = 0.pixels(alignOpposite = alignment.alignOpX, alignOutside = false)
                        y = 0.pixels(alignOpposite = alignment.alignOpY, alignOutside = false)
                        height = (if (alignment.centerY) 15 else 2).pixels()
                        width = (if (alignment.centerX) 15 else 2).pixels()
                    }
                }
            }

            onMouseEnter {
                children.forEach { it.animateColor(EssentialPalette.TEXT_HIGHLIGHT) }
            }
            onMouseLeave {
                children.forEach { it.animateColor(EssentialPalette.TEXT) }
            }
            var oldCrop: Crop? = null
            onMouseClick {
                dragging = true
                xDragOffset = it.relativeX + if (alignment.alignOpX) padding else -padding
                yDragOffset = it.relativeY + if (alignment.alignOpY) padding else -padding
                oldCrop = cropSettings.clone()
            }
            onMouseRelease {
                val wasDragging = dragging
                dragging = false
                if (oldCrop == cropSettings || !wasDragging) return@onMouseRelease
                vectorEditingOverlay.pushChange(
                    CropChange(oldCrop ?: return@onMouseRelease, cropSettings.clone())
                )
            }
            onMouseDrag { _, _, _ ->


                if (dragging) {

                    //When dragging the position items, we want to take the position inside the item into account
                    val (mouseX, mouseY) = getMousePosition()
                    val relativeX = mouseX - screenshotDisplay.getLeft()
                    val relativeY = mouseY - screenshotDisplay.getTop()

                    val adjustedMouseX =
                        if (alignment.alignOpX) (relativeX + (getWidth() - xDragOffset)) else relativeX - xDragOffset
                    val adjustedMouseY =
                        if (alignment.alignOpY) (relativeY + (getHeight() - yDragOffset)) else relativeY - yDragOffset

                    val (x, y) = getRelativeMousePosition(adjustedMouseX, adjustedMouseY)

                    val minSize = .1f
                    when (alignment) {
                        CropAlignment.TOP_LEFT -> {
                            cropSettings.top.set(y.coerceAtMost(cropSettings.bottom.get() - minSize))
                            cropSettings.left.set(x.coerceAtMost(cropSettings.right.get() - minSize))
                        }
                        CropAlignment.TOP_CENTER -> {
                            cropSettings.top.set(y.coerceAtMost(cropSettings.bottom.get() - minSize))
                        }
                        CropAlignment.TOP_RIGHT -> {
                            cropSettings.top.set(y.coerceAtMost(cropSettings.bottom.get() - minSize))
                            cropSettings.right.set(x.coerceAtLeast(cropSettings.left.get() + minSize))
                        }
                        CropAlignment.RIGHT_CENTER -> {
                            cropSettings.right.set(x.coerceAtLeast(cropSettings.left.get() + minSize))
                        }
                        CropAlignment.BOTTOM_RIGHT -> {
                            cropSettings.bottom.set(y.coerceAtLeast(cropSettings.top.get() + minSize))
                            cropSettings.right.set(x.coerceAtLeast(cropSettings.left.get() + minSize))
                        }
                        CropAlignment.BOTTOM_CENTER -> {
                            cropSettings.bottom.set(y.coerceAtLeast(cropSettings.top.get() + minSize))
                        }
                        CropAlignment.BOTTOM_LEFT -> {
                            cropSettings.bottom.set(y.coerceAtLeast(cropSettings.top.get() + minSize))
                            cropSettings.left.set(x.coerceAtMost(cropSettings.right.get() - minSize))
                        }
                        CropAlignment.LEFT_CENTER -> {
                            cropSettings.left.set(x.coerceAtMost(cropSettings.right.get() - minSize))
                        }
                    }
                    //We changed a state that can change the position of this item,
                    //Therefore, we want to call animationFrame to invalidate the cached
                    //x and y
                    animationFrame()
                }
            }
        }
    }

    /**
     * Returns the x / y percentage [mouseX] and [mouseY] are inside of [screenshotDisplay]
     */
    fun getRelativeMousePosition(mouseX: Float, mouseY: Float): Pair<Float, Float> {
        val width = screenshotDisplay.getWidth()
        val height = screenshotDisplay.getHeight()
        return (mouseX / width).coerceIn(0f..1f) to (mouseY / height).coerceIn(0f..1f)
    }

    /**
     * Called to reset the state of the editor and clear changes
     */
    fun reset() {
        cropSettings.reset()
        vectorEditingOverlay.reset()
    }


    enum class CropAlignment(
        val alignOpX: Boolean,
        val alignOpY: Boolean,
        val centerX: Boolean = false,
        val centerY: Boolean = false,
        val corner: Boolean
    ) {
        TOP_LEFT(alignOpX = false, alignOpY = false, corner = true),
        TOP_CENTER(alignOpX = false, alignOpY = false, centerX = true, corner = false),
        TOP_RIGHT(alignOpX = true, alignOpY = false, corner = true),
        RIGHT_CENTER(alignOpX = true, alignOpY = false, centerY = true, corner = false),
        BOTTOM_RIGHT(alignOpX = true, alignOpY = true, corner = true),
        BOTTOM_CENTER(alignOpX = false, alignOpY = true, centerX = true, corner = false),
        BOTTOM_LEFT(alignOpX = false, alignOpY = true, corner = true),
        LEFT_CENTER(alignOpX = false, alignOpY = false, centerY = true, corner = false);
    }

    data class Crop(
        var left: State<Float> = BasicState(0f),
        var right: State<Float> = BasicState(1f),
        var top: State<Float> = BasicState(0f),
        var bottom: State<Float> = BasicState(1f),
    ) {
        override fun toString(): String {
            return "Crop[left=${left.get()},right=${right.get()},top=${top.get()},bottom=${bottom.get()}]"
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Crop) return false
            return other.left.get() == left.get() &&
                    other.right.get() == right.get() &&
                    other.top.get() == top.get() &&
                    other.bottom.get() == bottom.get()
        }

        /**
         * Resets to default
         */
        fun reset() {
            left.set(0f)
            right.set(1f)
            top.set(0f)
            bottom.set(1f)
        }

        fun clone(): Crop {
            return Crop(BasicState(left.get()), BasicState(right.get()), BasicState(top.get()), BasicState(bottom.get()))
        }

        /**
         * Applies the values from another crop instance
         * @param other
         */
        fun copyFrom(other: Crop) {
            left.set(other.left.get())
            right.set(other.right.get())
            top.set(other.top.get())
            bottom.set(other.bottom.get())
        }
    }
}