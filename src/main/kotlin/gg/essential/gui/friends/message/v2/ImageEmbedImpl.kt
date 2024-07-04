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
package gg.essential.gui.friends.message.v2

import com.sparkuniverse.toolbox.chat.enums.ChannelType
import gg.essential.Essential
import gg.essential.api.gui.Slot
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.shadow.EssentialUIText
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.screenshot.constraints.AspectPreservingFillConstraint
import gg.essential.gui.util.hoveredState
import gg.essential.universal.ChatColor
import gg.essential.universal.UKeyboard
import gg.essential.util.*
import gg.essential.vigilance.utils.onLeftClick
import net.minecraft.client.Minecraft
import org.apache.commons.io.FileUtils
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile

class ImageEmbedImpl(
    url: URL,
    wrapper: MessageWrapper,
) : ImageEmbed(url, wrapper) {

    private val loadingState = BasicState(false)
    private var loadedImage: BufferedImage? = null
    private var loading: Int? = null

    private val aspectRatio = BasicState(9 / 16f)
    private val highlightedState = BasicState(false)
    private val focusedView by FocusedView()

    // Dummy component that stores the size of the image
    private val imageSizeContainer by UIContainer().constrain {
        width = MessageUtils.getMessageWidth(wrapper.channelType == ChannelType.ANNOUNCEMENT)
        height = AspectConstraint(9 / 16f)
    } hiddenChildOf wrapper

    private val loadingEmbed by UIBlock(EssentialPalette.LIGHT_DIVIDER).constrain {
        width = 100.percent boundTo imageSizeContainer
        height = 100.percent boundTo imageSizeContainer
    }.bindParent(this, loadingState)

    private val loadingIcon by LoadingIcon(2.0) childOf loadingEmbed

    init {

        constrain {
            x = 10.pixels(alignOpposite = wrapper.sentByClient)
            y = SiblingConstraint(4f)
            width = ChildBasedSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        if (loading == null) {
            prepareAndLoad()
        }

    }


    private fun prepareAndLoad() {
        loadingState.set(true)

        val nextId = nextLoadingId++
        loading = nextId

        val localPaths = MessageUtils.SCREENSHOT_URL_REGEX.find(url.toString())
            ?.let { Essential.getInstance().connectionManager.screenshotManager.getUploadedLocalPathsCache(it.groupValues[1]) }
            ?: emptyList()
        Multithreading.runAsync {
            loadImage(nextId, localPaths.firstNotNullOfOrNull { fetchLocalImage(it) } ?: fetchRemoteImage())
        }
    }

    override fun copyImageToClipboard() {
        val loadedImage = loadedImage
        if (loadedImage != null && loadedImage != noImage) {
            Multithreading.runAsync {
                val tempFile = Files.createTempFile("essential-screenshot", "png").toFile()
                ImageIO.write(loadedImage, "png", tempFile)
                Minecraft.getMinecraft().executor.execute {
                    Essential.getInstance().connectionManager.screenshotManager.copyScreenshotToClipboardWithMessage(
                        tempFile,
                        "Successfully copied image to clipboard."
                    )
                    // Cleanup temp file
                    FileUtils.deleteQuietly(tempFile)
                }
            }
        }
    }

    override fun saveImageToScreenshotBrowser() {
        val loadedImage = loadedImage
        if (loadedImage != null && loadedImage != noImage) {
            val future = Essential.getInstance().connectionManager.screenshotManager.saveDownloadedImageAsync(loadedImage)
            future.whenComplete { _, throwable ->
                if (throwable == null) {
                    Notifications.push(
                        "Picture saved", "",
                        action = { GuiUtil.openScreen { ScreenshotBrowser() } }
                    ) {
                        withCustomComponent(Slot.ICON, EssentialPalette.PICTURES_SHORT_9X7.create())
                    }
                }
            }
        }
    }

    override fun beginHighlight() {
        highlightedState.set(true)
    }

    override fun releaseHighlight() {
        highlightedState.set(false)
    }

    private fun download(): BufferedImage? {
        val original = WebUtil.downloadToBytes(url.toString(), "Essential Embeds")

        try {
            ImageIO.read(ByteArrayInputStream(original))?.let { return it }
        } catch (e: Exception) {
            Essential.logger.debug("Error parsing image", e)
        }

        // Follow metadata if present
        val responseString = String(original)
        val embedUrlMatcher = MessageUtils.imageEmbedRegex.find(responseString) ?: return noImage
        val embedUrl = embedUrlMatcher.groups["url"]?.value ?: return noImage
        if (!MessageUtils.URL_REGEX.matches(embedUrl)) {
            return noImage
        }

        val embedUrlBytes = WebUtil.downloadToBytes(embedUrl, "Essential Embeds")

        return try {
            ImageIO.read(ByteArrayInputStream(embedUrlBytes))
        } catch (e: Exception) {
            Essential.logger.debug("Error parsing image", e)
            null
        }
    }

    /**
     * Attempts to fetch local image and returns BufferedImage if successful
     */
    private fun fetchLocalImage(localPath: Path): BufferedImage? {
        return try {
            if (!localPath.isRegularFile()) {
                Essential.logger.debug("Local image path does not point to a file: {}", localPath)
                return null
            }
            localPath.inputStream().use { ImageIO.read(it) }
        } catch (e: Exception) {
            Essential.logger.error("Error loading local image using path: $localPath", e)
            null
        }
    }

    /**
     * Attempts to download remote image and returns BufferedImage if successful
     */
    private fun fetchRemoteImage(): BufferedImage? {
        return try {
            download()
        } catch (e: IOException) {
            Essential.logger.debug("Error downloading image", e)
            null
        }
    }

    private fun loadImage(id: Int, loadedImage: BufferedImage?) {
        maybeLoadUIImage(loadedImage) { uiImage ->
            if (id != loading) {
                return@maybeLoadUIImage
            }

            loadingState.set(false)

            this.loadedImage = loadedImage

            if (loadedImage == noImage) {
                return@maybeLoadUIImage
            }

            val imageContainer by UIContainer().constrain {
                width = ChildBasedMaxSizeConstraint()
                height = ChildBasedMaxSizeConstraint()
            } childOf this

            if (uiImage == null) {
                FailedEmbed() childOf imageContainer
            } else {
                loadedImage?.let { aspectRatio.set(it.width / it.height.toFloat()) }

                displayImageEmbed(imageContainer, uiImage)
            }

        }
    }

    private var previewing = false


    private fun getWindow(): Window {
        return Window.of(this)
    }

    inner class FocusedView : UIContainer() {

        private val block = UIBlock(Color(0, 0, 0, 0)).constrain {
            height = 100.percent
            width = 100.percent
        } childOf this

        private val floatImageContainer by UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 1.pixel
            height = 1.pixel
        } childOf this

        private val floatImage by generateEmptyImage().centered().constrain {
            width = AspectPreservingFillConstraint(aspectRatio)
            height = AspectPreservingFillConstraint(aspectRatio)
        }.apply {
            textureMagFilter = UIImage.TextureScalingMode.LINEAR
            textureMinFilter = UIImage.TextureScalingMode.LINEAR
            onKeyType { _, keyCode ->
                if (keyCode == UKeyboard.KEY_ESCAPE) {
                    exitPreview()
                }
            }
            onMouseClick {
                grabWindowFocus()
                it.stopImmediatePropagation()
            }
        } childOf floatImageContainer

        private val openOriginal by EssentialUIText().constrain {
            y = SiblingConstraint(3f) boundTo floatImage
            x = 0.pixels boundTo floatImage
            color = Color(0, 0, 0, 0).toConstraint()
        }.apply {
            bindText(hoveredState().map {
                if (it) {
                    "${ChatColor.UNDERLINE}"
                } else {
                    ""
                } + "Open Original"
            })
        }.onLeftClick {
            OpenLinkModal.openUrl(url.toURI())
        } childOf this

        init {
            constrain {
                width = 100.percent
                height = 100.percent
            }
            onLeftClick {
                exitPreview()
            }
        }

        fun setup(image: UIImage) {
            image.supply(floatImage)
        }

        fun enterPreview() {
            if (previewing) {
                return
            }

            previewing = true
            getWindow().addChild(this)
            floatImage.grabWindowFocus()
            block.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, Color(0, 0, 0, 200).toConstraint())
            }
            openOriginal.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, EssentialPalette.TEXT_HIGHLIGHT.toConstraint())
            }
            floatImageContainer.animate {
                setWidthAnimation(Animations.OUT_EXP, animationTime, 75.percentOfWindow)
                setHeightAnimation(Animations.OUT_EXP, animationTime, 75.percentOfWindow)
            }
        }

        private fun exitPreview(callback: () -> Unit = {}) {
            releaseWindowFocus()

            floatImageContainer.animate {
                setWidthAnimation(Animations.OUT_EXP, animationTime, 1.pixel)
                setHeightAnimation(Animations.OUT_EXP, animationTime, 1.pixel)
            }
            openOriginal.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, Color(0, 0, 0, 0).toConstraint())
            }

            block.animate {
                setColorAnimation(Animations.OUT_EXP, animationTime, Color(0, 0, 0, 0).toConstraint()).onComplete {
                    callback()
                    previewing = false
                    this@FocusedView.hide(instantly = true)
                }
            }
        }

        private fun generateEmptyImage(): UIImage {
            return UIImage(CompletableFuture.completedFuture(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)))
        }
    }

    inner class FailedEmbed : UIBlock(EssentialPalette.getMessageColor(hoveredState() or messageWrapper.appearHovered.toV1(this@ImageEmbedImpl), false)) {
        init {
            layoutAsBox(Modifier.childBasedWidth().childBasedHeight()) {
                column(Modifier.childBasedWidth(10f).childBasedHeight(8f), Arrangement.spacedBy(0f, FloatPosition.CENTER)) {
                    spacer(height = 1f) // button shadow
                    row(Arrangement.spacedBy(10f)) {
                        text("Image failed to load.", Modifier.shadow(EssentialPalette.TEXT_SHADOW))
                        retryButton(Modifier.width(17f).heightAspect(1f))
                    }
                }
            }
        }
    }

    private fun displayImageEmbed(container: UIContainer, image: UIImage) {
        image.apply {
            textureMagFilter = UIImage.TextureScalingMode.LINEAR
            textureMinFilter = UIImage.TextureScalingMode.LINEAR
            focusedView.setup(this)
        }.onLeftClick {
            focusedView.enterPreview()
        }.onMouseClick {
            if (it.mouseButton != 1) return@onMouseClick
            messageWrapper.openOptionMenu(it, this@ImageEmbedImpl)
        }

        val borderModifier = Modifier.color(EssentialPalette.MESSAGE_HIGHLIGHT)
        val sideBorderModifier = borderModifier.width(3f).fillHeight()
        val topBottomBorderModifier = borderModifier.height(3f).fillWidth()

        container.layout {
            box(
                Modifier.then(BasicHeightModifier { AspectPreservingFillConstraint(aspectRatio) boundTo imageSizeContainer })
                    .then(BasicWidthModifier { AspectPreservingFillConstraint(aspectRatio) boundTo imageSizeContainer })
            ) {
                image(Modifier.fillParent())
                if_(highlightedState) {
                    box(sideBorderModifier.alignHorizontal(Alignment.Start))
                    box(sideBorderModifier.alignHorizontal(Alignment.End))
                    box(topBottomBorderModifier.alignVertical(Alignment.Start))
                    box(topBottomBorderModifier.alignVertical(Alignment.End))
                }
            }
        }
    }

    private fun LayoutScope.retryButton(modifier: Modifier = Modifier) {
        IconButton(EssentialPalette.RETRY_7X)(modifier.shadow(Color.BLACK).onLeftClick {
            this@ImageEmbedImpl.clearChildren()
            prepareAndLoad()
        }).apply {
            setColor(hoveredState().map { if (it) EssentialPalette.BUTTON else EssentialPalette.COMPONENT_BACKGROUND }.toConstraint())
        }
    }

    companion object {
        private var nextLoadingId: Int = 0
        private const val animationTime = 0.25f

        // Used to denote that no image should be displayed. This is returned by download()
        // when the web page is successfully fetched, but no image is found.
        private val noImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    }
}
