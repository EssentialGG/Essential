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

import gg.essential.Essential
import gg.essential.elementa.UIComponent
import gg.essential.gui.elementa.state.v2.MutableListState
import gg.essential.gui.elementa.state.v2.collections.TrackedList
import gg.essential.gui.elementa.state.v2.mutableListStateOf
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.RemoteScreenshot
import gg.essential.gui.screenshot.ScreenshotId
import gg.essential.gui.screenshot.bytebuf.LimitedAllocator
import gg.essential.gui.screenshot.bytebuf.WorkStealingAllocator
import gg.essential.gui.screenshot.components.ScreenshotProperties
import gg.essential.gui.screenshot.components.ScreenshotProviderManager
import gg.essential.gui.screenshot.concurrent.PrioritizedCallable
import gg.essential.gui.screenshot.concurrent.PriorityThreadPoolExecutor
import gg.essential.gui.screenshot.image.ScreenshotImage
import gg.essential.gui.screenshot.providers.FileCachedWindowedImageProvider
import gg.essential.gui.screenshot.providers.MaxScopeExpansionWindowProvider
import gg.essential.gui.screenshot.providers.MinecraftWindowedTextureProvider
import gg.essential.gui.screenshot.providers.PriorityDelegatedWindowProvider
import gg.essential.gui.screenshot.providers.ScopeExpansionWindowProvider
import gg.essential.gui.screenshot.providers.ScopePreservingWindowedProvider
import gg.essential.gui.screenshot.providers.ThreadedWindowedProvider
import gg.essential.gui.screenshot.providers.TransitionWindowedProvider
import gg.essential.gui.screenshot.providers.WindowedProvider
import gg.essential.gui.screenshot.providers.WindowedTextureProvider
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.universal.UMinecraft
import gg.essential.util.findChildOfTypeOrNull
import io.netty.buffer.PooledByteBufAllocator
import net.minecraft.util.ResourceLocation
import java.util.concurrent.TimeUnit

class SimpleScreenshotProvider {

    val screenshotManager = Essential.getInstance().connectionManager.screenshotManager
    private val nThread = Runtime.getRuntime().availableProcessors() * 4
    private val pool = PriorityThreadPoolExecutor(nThread)
    private val nonBlockingAllocator =
        LimitedAllocator(PooledByteBufAllocator.DEFAULT, ScreenshotProviderManager.MAX_MEMORY)
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

    private var targetPreviewImageSize = ScreenshotProviderManager.minResolutionTargetResolution

    private val minResolutionBicubicProvider =
        createFileCachedBicubicProvider(ScreenshotProviderManager.minResolutionTargetResolution)
    private val minResolutionMinecraftWindowedTextureProvider = MinecraftWindowedTextureProvider(
        ThreadedWindowedProvider(
            minResolutionBicubicProvider,
            pool,
            PrioritizedCallable.MIN_RES,
        )
    )

    private val scopePreservedMinResolutionProvider = ScopePreservingWindowedProvider(
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

    // The screenshots in the current view
    // Setup in reloadItems()
    var currentPaths: List<ScreenshotId> = listOf()
        set(value) {
            provider.items = value
            field = value
        }

    // The screenshots that will be used for this view of the screenshot folder
    // In the event a new screenshot is taken, we will need to update this or reload the GUI
    val allPaths: MutableListState<ScreenshotId> = mutableListStateOf()

    var imageMap: Map<ScreenshotId, ResourceLocation> = mapOf()

    init {

        pool.setKeepAliveTime(10, TimeUnit.SECONDS)
        pool.allowCoreThreadTimeOut(true)

        reloadItems()
    }

    /**
     * Reloads the paths because of a newly taken screenshot or external changes
     */
    fun reloadItems() {
        val screenshotManager = Essential.getInstance().connectionManager.screenshotManager
        val remoteMedia = screenshotManager.uploadedMedia.associateBy { it.id }.toMutableMap()
        val localScreenshots = screenshotManager.orderedPaths
            .map { path ->
                val id = LocalScreenshot(path)
                val metadata = screenshotManager.screenshotMetadataManager.getMetadata(path)
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
        allPaths.set { it.applyChanges(TrackedList.Change.estimate(it, localScreenshots + remoteScreenshots)) }
    }

    /**
     * Called to clean up resources on close
     */
    fun cleanup() {
        // Call with empty windows to clean up any allocated textures
        provider.provide(emptyList(), emptySet())
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
     * Runs on each frame
     */
    fun frameUpdate(componentWithImages: UIComponent) {
        updateResolution(componentWithImages)
        imageMap = provide()
        renderedLastFrame = null
    }

    /**
     * Update the resolution for all provided images
     */
    private fun updateResolution(componentWithImages: UIComponent) {
        val preview = componentWithImages.findChildOfTypeOrNull<ScreenshotImage>(recursive = true)

        var resolution = if (preview == null) {
            ScreenshotProviderManager.minResolutionTargetResolution
        } else {
            val realWidth = (preview.getWidth() * UMinecraft.guiScale).toInt()
            val realHeight = (preview.getHeight() * UMinecraft.guiScale).toInt()
            Pair(realWidth, realHeight)
        }
        resolution = roundResolutionToCommonValues(resolution)

        if (targetPreviewImageSize == resolution) {
            return
        }
        targetPreviewImageSize = resolution

        val newTargetProvider = createWindowedTextureProvider(resolution)
        val currentTargetProvider = providerArray[0]

        //Debug log to help track down any user experienced performance issues
        Essential.logger.debug("Updating provider to target resolution {}", resolution)

        val transitionWindowedProvider = TransitionWindowedProvider(newTargetProvider, currentTargetProvider)
        transitionWindowedProvider.items = currentPaths
        providerArray[0] = transitionWindowedProvider
    }

    private fun createFileCachedBicubicProvider(targetResolution: Pair<Int, Int>): FileCachedWindowedImageProvider =
        ScreenshotProviderManager.createFileCachedBicubicProvider(
            targetResolution,
            pool,
            allocator,
            Essential.getInstance().baseDir,
            screenshotManager.nativeImageReader
        )

    private fun createWindowedTextureProvider(resolution: Pair<Int, Int>): WindowedTextureProvider {
        return ScopeExpansionWindowProvider(
            MinecraftWindowedTextureProvider(
                ThreadedWindowedProvider(
                    createFileCachedBicubicProvider(roundResolutionToCommonValues(resolution)),
                    pool,
                    PrioritizedCallable.REGULAR
                ),
            ),
            1f,
        )
    }

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

}