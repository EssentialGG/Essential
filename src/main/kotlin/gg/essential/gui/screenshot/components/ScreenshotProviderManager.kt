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

import gg.essential.Essential
import gg.essential.api.gui.Slot
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixel
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.elementaDev
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.shadow.ShadowIcon
import gg.essential.gui.notification.Notifications
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.bytebuf.LimitedAllocator
import gg.essential.gui.screenshot.bytebuf.WorkStealingAllocator
import gg.essential.gui.screenshot.concurrent.PrioritizedCallable
import gg.essential.gui.screenshot.concurrent.PriorityThreadPoolExecutor
import gg.essential.gui.screenshot.providers.*
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.universal.UMinecraft
import gg.essential.util.GuiUtil
import gg.essential.util.findChildOfTypeOrNull
import gg.essential.util.lwjgl3.api.NativeImageReader
import gg.essential.gui.util.pollingState
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Manages the staged image loading providers for the Screenshot Manager
 */
class ScreenshotProviderManager(
    private val browser: ScreenshotBrowser,
    private val scroller: ScreenshotScrollComponent,
) {

    private val nativeImageReader = browser.screenshotManager.nativeImageReader
    private val window = browser.window
    private val nThread = Runtime.getRuntime().availableProcessors() * 4
    private val pool = PriorityThreadPoolExecutor(nThread)
    private val nonBlockingAllocator = LimitedAllocator(PooledByteBufAllocator.DEFAULT, MAX_MEMORY)
    private val allocator = WorkStealingAllocator(nonBlockingAllocator) {
        val task = pool.stealBackgroundTask()
        if (task != null) {
            task.run()
        } else {
            Thread.sleep(1) // wait for other threads to make progress
        }
    }
    val propertyMap = mutableMapOf<ScreenshotId, ScreenshotProperties>()
    var renderedLastFrame: WindowedProvider.Window? = null

    private val targetPreviewImageSize = window.pollingState(minResolutionTargetResolution) {
        val preview = scroller.findChildOfTypeOrNull<ScreenshotPreview>(recursive = true)
            ?: return@pollingState minResolutionTargetResolution
        val realWidth = (preview.getWidth() * UMinecraft.guiScale).toInt()
        val realHeight = (preview.getHeight() * UMinecraft.guiScale).toInt()
        roundResolutionToCommonValues(Pair(realWidth, realHeight))
    }

    private val targetFocusImageSize = window.pollingState(minResolutionTargetResolution) {
        val realWidth = (window.getWidth() * 0.57 * UMinecraft.guiScale).toInt()
        // Using a generous upper bound for the height, width is usually the limiting factor anyway
        val realHeight = (window.getHeight() * UMinecraft.guiScale).toInt()
        Pair(realWidth, realHeight)
    }

    private val minResolutionBicubicProvider = createFileCachedBicubicProvider(minResolutionTargetResolution)
    private var focusImageResolution = createFocusImageProvider(minResolutionTargetResolution)
    private val minResolutionMinecraftWindowedTextureProvider = MinecraftWindowedTextureProvider(
        ThreadedWindowedProvider(
            minResolutionBicubicProvider,
            pool,
            PrioritizedCallable.MIN_RES,
        )
    )

    private val scopePreservedMinResolutionProvider =  ScopePreservingWindowedProvider(
        MaxScopeExpansionWindowProvider(
            minResolutionMinecraftWindowedTextureProvider
        )
    )

    private val providerArray: Array<WindowedProvider<ResourceLocation>> = arrayOf(

        // First item is the primary and provider
        // This is updated to be the target resolution when the target resolution changes
        createWindowedTextureProvider(Pair(200, 200)),

        // Fallback to the lowest resolution if the target resolution is not available
        // Expand the scope of the max to keep everything in the scope
        scopePreservedMinResolutionProvider
    )

    // The actual provider list view items are queried from
    private val provider = PriorityDelegatedWindowProvider(providerArray)

    // The screenshots in the current view [gg.essential.gui.screenshot.components.Tab]
    // Setup in reloadItems()
    var currentPaths: List<ScreenshotId> = listOf()

    // The screenshots that will be used for this view of the screenshot folder
    // In the event a new screenshot is taken, we will need to update this or reload the GUI
    var allPaths: List<ScreenshotId> = listOf()

    /**
     * Finds the nearest resolution with no less than 30% difference from our value
     * Candidate resolutions are found by starting at 200 and growing by 30%
     */
    private fun roundResolutionToCommonValues(targetResolution: Pair<Int, Int>): Pair<Int, Int> {
        val (width, height) = targetResolution
        return Pair(roundResolutionToCommonValues(width), roundResolutionToCommonValues(height))
    }

    private fun roundResolutionToCommonValues(targetResolution: Int): Int {
        val minResolution = 200f //Roughly native resolution for images being rendered on a 1080p monitor with 7 per row
        val acceptableScaling = .3f //Percentage the output resolution can be at most wrong by

        var outputLTInput = minResolution //output resolution one increment smaller than the targetResolution

        //output resolution one increment greater than the targetResolution
        var outputGTInput = minResolution * (1f + acceptableScaling)

        while (outputGTInput < targetResolution) {
            outputLTInput = outputGTInput
            outputGTInput *= (1 + acceptableScaling)
        }


        //Choose the resolution that we are closer to
        val deltaLt = targetResolution - outputLTInput
        val deltaGt = outputGTInput - targetResolution

        return if (deltaLt < deltaGt) outputLTInput.toInt() else outputGTInput.toInt()
    }

    private fun createFileCachedBicubicProvider(targetResolution: Pair<Int, Int>): FileCachedWindowedImageProvider =
        createFileCachedBicubicProvider(targetResolution, pool, allocator, Essential.getInstance().baseDir, nativeImageReader)

    private fun createFocusImageProvider(targetResolution: Pair<Int, Int>): WindowedTextureProvider {
        return MinecraftWindowedTextureProvider(
            ThreadedWindowedProvider(
                createFileCachedBicubicProvider(targetResolution),
                pool,
                PrioritizedCallable.FOCUS,
            )
        )
    }

    private fun createWindowedTextureProvider(resolution: Pair<Int, Int>): WindowedTextureProvider {
        return ScopeExpansionWindowProvider(
            MinecraftWindowedTextureProvider(
                ThreadedWindowedProvider(
                    createFileCachedBicubicProvider(roundResolutionToCommonValues(resolution)), pool, PrioritizedCallable.REGULAR
                ),
            ),
            1f,
        )
    }

    init {
        reloadItems()
        updateItems(allPaths)
        provider.items = currentPaths
        focusImageResolution.items = currentPaths

        targetFocusImageSize.onSetValue {
            focusImageResolution = TransitionWindowedProvider(createFocusImageProvider(it), focusImageResolution)
            focusImageResolution.items = currentPaths
        }

        targetPreviewImageSize.onSetValue {
            val newTargetProvider = createWindowedTextureProvider(it)
            val currentTargetProvider = providerArray[0]

            //Debug log to help track down any user experienced performance issues
            Essential.logger.debug("Updating provider to target resolution $it")


            val transitionWindowedProvider = TransitionWindowedProvider(newTargetProvider, currentTargetProvider)
            transitionWindowedProvider.items = currentPaths
            providerArray[0] = transitionWindowedProvider
        }

        if (elementaDev || System.getProperty("essential.debugScreenshots", "false") == "true") {
            val window = browser.window
            val text = window.pollingState { "${(nonBlockingAllocator.getAllocatedBytes() / 1024)} KB" }
            UIText().bindText(text).constrain {
                x = 5.pixels(alignOpposite = true)
                y = 5.pixels
            } childOf window
        }
    }

    /**
     * Reloads the paths because of an addition by saving an edit
     */
    fun reloadItems() {
        // Provider items updated in updateItems which is called by the ListViewComponent's reload
        val screenshotManager = Essential.getInstance().connectionManager.screenshotManager
        val remoteMedia = screenshotManager.uploadedMedia.associateBy { it.id }.toMutableMap()
        val localScreenshots = screenshotManager.orderedPaths
            .map { path ->
                val id = LocalScreenshot(path)
                val metadata = browser.screenshotManager.screenshotMetadataManager.getMetadata(path)
                propertyMap[id] = ScreenshotProperties(id, metadata)
                metadata?.mediaId?.let { remoteMedia.remove(it) }
                id
            }
        val remoteScreenshots = remoteMedia.values
            .map { media ->
                val id = RemoteScreenshot(media)
                val metadata = ClientScreenshotMetadata(media)
                propertyMap[id] = ScreenshotProperties(id, metadata)
                id
            }
        allPaths = localScreenshots + remoteScreenshots
    }

    /**
     * Sets the current paths and the providers to the provided list
     */
    fun updateItems(paths: List<ScreenshotId>): Map<ScreenshotId, ResourceLocation> {
        provider.items = paths
        focusImageResolution.items = paths

        this.currentPaths = paths

        // provide() is only called, and therefore provider is only updated, while the list
        // view is active because that's the only thing which needs it. This in turn means
        // that if we delete an edited image, and then re-edit the original to re-create a
        // new edited image at the exact same location, all without leaving focus view, then
        // provider will never get a chance to flush the old image from its cache. It doesn't
        // know that the underlying file content has changed, and by the time its provide method
        // is called, its items is effectively unchanged.
        // We can mitigate this by forcing a call to provide every time the items are changed,
        // and therefore also right after an item is deleted, regardless of which view is currently active.

        return provide()
    }

    /**
     * Called to clean up resources on close
     */
    fun cleanup() {
        // Call with empty windows to clean up any allocated textures
        provider.provide(emptyList(), emptySet())
        focusImageResolution.provide(emptyList(), emptySet())
        pool.shutdown()
    }

    /**
     * Queries [provider] with [renderedLastFrame] and returns the result
     */
    fun provide(): Map<ScreenshotId, ResourceLocation> {
        val windows = listOfNotNull(renderedLastFrame?.inRange(provider.items.indices))
        if (windows.isEmpty()) {
            // If we call the provider with an empty list, it will unnecessarily clean up all resources.
            // To prevent this, we skip the call in that case and just return an empty map.
            return emptyMap()
        }
        return provider.provide(windows, emptySet())
    }

    /**
     * Queries the [focusImageResolution] with the specified window and returns the result
     */
    fun provideFocus(window: WindowedProvider.Window): Map<ScreenshotId, ResourceLocation> {
        return provideFocus(listOf(window))
    }

    /**
     * Queries the [focusImageResolution] with the specified windows and returns the result
     */
    fun provideFocus(windows: List<WindowedProvider.Window>): Map<ScreenshotId, ResourceLocation> {
        return focusImageResolution.provide(windows, emptySet())
    }

    /**
     * Handles a screenshot being deleted
     */
    fun handleDelete(properties: ScreenshotProperties, uploadedOnly: Boolean = false, delete: () -> Unit = {}) {
        GuiUtil.pushModal { manager -> 
            DangerConfirmationEssentialModal(manager, "Delete", requiresButtonPress = false).configure {
                contentText =
                    if (uploadedOnly) {
                        "Are you sure you want to remove the upload of ${properties.id.name}?\n" +
                            "This will invalidate all links to the image."
                    } else {
                        "Are you sure you want to delete ${properties.id.name}?"
                    }
            }.onPrimaryAction {
                Window.enqueueRenderOperation {
                    if (properties.id is LocalScreenshot && !uploadedOnly) {
                        browser.screenshotManager.handleDelete(properties.id.path.toFile(), false)
                    }
                    val mediaId = properties.metadata?.mediaId
                    if (mediaId != null) {
                        browser.screenshotManager.deleteMedia(mediaId, (properties.id as? LocalScreenshot)?.path)
                    }
                    // If we're deleting not just the uploaded variant, or the uploaded variant is the only variant,
                    // then we also need to remove the component from the list, otherwise we don't.
                    val deleteComponent = !uploadedOnly || properties.id is RemoteScreenshot
                    if (deleteComponent) {
                        scopePreservedMinResolutionProvider.handleDelete(properties.id)
                        browser.stateManager.handleDelete(properties)
                    }
                    // We do in any case have to reload though, so the removal of the uploaded variant gets propagated
                    reloadItems()
                    browser.listViewComponent.reload()
                    if (deleteComponent) {
                        delete()
                    } else {
                        // FIXME ScreenshotProperties is bad, that should have been a state but instead it's a class
                        //       we manually have to update everywhere and we can't even just update the top-level state
                        //       because that class does equals by id only so it won't update when the properties have
                        //       changed.
                        //       Cannot be bothered to fix that everywhere now, so for now I'll just manually update
                        //       the one special case where it currently breaks, deleting uploadedOnly in focused view:
                        if (browser.focusing.get() == properties) {
                            browser.focusing.set(null)
                            browser.changeFocusedComponent(propertyMap[properties.id] ?: return@enqueueRenderOperation)
                        }
                    }
                    if (uploadedOnly) {
                        // FIXME should probably be done by the caller and only if the packet succeeds, but without
                        //       coroutines that's callback hell (and we already have one callback, so let's not)
                        Notifications.push("Upload has been removed.", "") {
                            val icon =
                                ShadowIcon(EssentialPalette.CHECKMARK_7X5, true)
                                    .constrain { y = 1.pixels }
                                    .rebindPrimaryColor(BasicState(Color.WHITE))
                                    .rebindShadowColor(BasicState(EssentialPalette.MODAL_OUTLINE))
                            val container = UIContainer()
                                .constrain {
                                    width = ChildBasedSizeConstraint() + 1.pixel
                                    height = ChildBasedSizeConstraint() + 2.pixels
                                }
                                .addChild(icon)
                            withCustomComponent(Slot.PREVIEW, container)
                        }
                    }
                }
            }
        }
    }

    /**
     * Notifies this instance that the supplied paths have been deleted externally
     * and must be removed from the providers
     */
    fun externalDelete(paths: Set<Path>) {
        for (path in paths) {
            scopePreservedMinResolutionProvider.handleDelete(LocalScreenshot(path))
        }
        reloadItems()
    }

    companion object {

        /**
         * The resolution of the smallest down sampled size that screenshots are available at
         */
        @JvmField
        val minResolutionTargetResolution = Pair(40, 40)

        val MAX_MEMORY = (System.getProperty("essential.screenshots.max_mem_mb")?.toLong() ?: 100) * 1_000_000

        @JvmOverloads
        fun createFileCachedBicubicProvider(
            targetResolution: Pair<Int, Int>,
            pool: PriorityThreadPoolExecutor,
            alloc: ByteBufAllocator,
            essentialDir: File,
            nativeImageReader: NativeImageReader,
            precomputeOnly: Boolean = false,
        ): FileCachedWindowedImageProvider {
            val (targetWidth, targetHeight) = targetResolution
            return FileCachedWindowedImageProvider(
                PostProcessWindowedImageProvider(
                    CloudflareImageProvider(
                        DiskWindowedImageProvider(nativeImageReader, alloc),
                        nativeImageReader,
                        alloc,
                        targetResolution,
                    ),
                    PostProcessWindowedImageProvider.bicubicFilter(targetWidth, targetHeight)
                ),
                FileCachedWindowedImageProvider.inDirectory(
                    essentialDir.toPath().resolve("screenshot-cache")
                        .resolve("bicubic_${targetWidth}x$targetHeight")
                        .also(Files::createDirectories)
                ),
                pool,
                nativeImageReader,
                alloc,
                precomputeOnly
            )
        }
    }

}
