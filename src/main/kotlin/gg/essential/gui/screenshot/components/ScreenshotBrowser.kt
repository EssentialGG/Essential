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
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.state.BasicState
import gg.essential.gui.InternalEssentialGUI
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.modal.PropertiesModal
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.handlers.screenshot.ClientScreenshotMetadata
import gg.essential.network.connectionmanager.media.ScreenshotCollectionChangeEvent
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import gg.essential.util.*
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import net.minecraft.client.Minecraft
import java.nio.file.Path
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

class ScreenshotBrowser(editPath: Path? = null): InternalEssentialGUI(
    ElementaVersion.V6,
    "Pictures",
    discordActivityDescription = "Browsing pictures",
) {

    private val closeOperations = ConcurrentLinkedQueue<() -> Unit>()

    val focusing = BasicState<ScreenshotProperties?>(null)
    val currentView: BasicState<View> = focusing.map {
        if (it != null) {
            View.FOCUS
        } else {
            View.LIST
        }
    }
    val focusType: BasicState<FocusType> = BasicState(FocusType.VIEW)

    val screenshotManager = Essential.getInstance().connectionManager.screenshotManager
    val stateManager = ScreenshotStateManager(this.screenshotManager)
    val listViewComponent = ListViewComponent(this)
    val providerManager = ScreenshotProviderManager(this, listViewComponent.screenshotScrollComponent)
    val focusListComponent = FocusListComponent(this)
    val focusEditComponent = FocusEditComponent(this)

    val menuDialogOwner = BasicState<ScreenshotProperties?>(null)
    val optionsDropdown = ScreenshotOptionsDropdown(this, menuDialogOwner)

    private var stateToRestore = View.LIST

    private val refreshHandler: Consumer<ScreenshotCollectionChangeEvent> = Consumer {
        if (it.screenshotsDeleted()) {
            // externalDelete will call refresh
            externalDelete(it.deletedPaths)
        } else if (it.screenshotsCreated()) {
            providerManager.reloadItems()
            listViewComponent.reload()
        }
    }

    init {
        titleText.bindParent(titleBar, listViewComponent.active)


        currentView.map { it == View.LIST }.onSetValue {
            backButtonVisible = it
        }

        // Clear the default key handler and override so that hitting escape doesn't unconditionally close the UI
        window.keyTypedListeners.removeFirst()
        window.onKeyType { typedChar, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE && onEscapeButtonPressed()) {
                return@onKeyType
            }
            defaultKeyBehavior(typedChar, keyCode)
        }

        // Open the editor to a specific file if supplied
        if (editPath != null) {
            openEditor(
                ScreenshotProperties(
                    LocalScreenshot(editPath),
                    screenshotManager.screenshotMetadataManager.getMetadata(editPath.toFile())
                )
            )
        }

        screenshotManager.registerScreenshotCollectionChangeHandler(refreshHandler)
    }

    /**
     * Focus the screenshot browser around the specified item
     */
    fun openFocusView(properties: ScreenshotProperties) {
        changeFocusedComponent(properties)
        focusType.set(FocusType.VIEW)
    }

    fun changeFocusedComponent(properties: ScreenshotProperties) {
        focusing.set(properties)
        focusListComponent.beginPreview(properties)
    }

    fun displayPropertiesModal(properties: ScreenshotProperties) {
        val metadata = generateImageProperties(properties)
        GuiUtil.pushModal { manager -> PropertiesModal(manager, metadata.toMap()) }
    }

    private fun generateImageProperties(properties: ScreenshotProperties): List<Pair<String, State<String>>> {
        val pairList = mutableListOf<Pair<String, State<String>>>()
        pairList.add("Name" to stateOf(properties.id.name))
        val metadata = properties.metadata
        if (metadata != null) {
            val locationIdentifier = metadata.locationMetadata.identifier ?: ""
            when (metadata.locationMetadata.type) {
                ClientScreenshotMetadata.Location.Type.UNKNOWN -> pairList.add("Location" to stateOf("Unknown"))
                ClientScreenshotMetadata.Location.Type.MULTIPLAYER -> pairList.add("Server" to stateOf(locationIdentifier))
                ClientScreenshotMetadata.Location.Type.MENU -> pairList.add("Menu" to stateOf(locationIdentifier))
                ClientScreenshotMetadata.Location.Type.SINGLE_PLAYER -> pairList.add("World" to stateOf(locationIdentifier))
                ClientScreenshotMetadata.Location.Type.SHARED_WORLD -> {
                    val host = Essential.getInstance().connectionManager.spsManager.getHostFromSpsAddress(locationIdentifier)
                    if (host != null) {
                        pairList.add("Location" to stateOf("Shared World"))
                        pairList.add("Host" to UuidNameLookup.nameState(host, "Loading..."))
                    } else {
                        pairList.add("Location" to stateOf(locationIdentifier))
                    }
                }
            }
            pairList.add("Creator" to UuidNameLookup.nameState(metadata.authorId, "Loading..."))
        }
        val imageTime = getImageTime(properties, false)
        if (imageTime.time > 0) {
            val instant = Instant.ofEpochMilli(imageTime.time)
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            pairList.add("Date" to stateOf(imageTime.format(SimpleDateFormat("EEEE", Locale.ENGLISH)) + ", " + formatDate(localDate)))
            pairList.add("Time" to stateOf(formatTime(instant, true)))
        }
        val editTime = properties.metadata?.editTime
        if (editTime != null) {
            val instant = Instant.ofEpochMilli(editTime.time)
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            pairList.add("Edit Date" to stateOf(editTime.format(SimpleDateFormat("EEEE", Locale.ENGLISH)) + ", " + formatDate(localDate)))
            pairList.add("Edit Time" to stateOf(formatTime(instant, true)))
        }
        val dimension = mutableStateOf("Loading...")
        Multithreading.runAsync {
            val text = try {
                val bytes = Unpooled.wrappedBuffer(properties.id.open().use { it.readBytes() })
                val image = screenshotManager.nativeImageReader.getImageData(bytes, UnpooledByteBufAllocator.DEFAULT)
                image.release()

                "${image.width}x${image.height} pixels"
            } catch (e: Exception) {
                Essential.logger.error("Failed to read image dimensions: ", e)
                "Unknown"
            }

            UMinecraft.getMinecraft().executor.execute {
                dimension.set(text)
            }
        }
        pairList.add("Dimensions" to dimension)
        return pairList
    }

    /**
     * Opens the editor to the specified item
     */
    fun openEditor(properties: ScreenshotProperties) {
        stateToRestore = currentView.get()
        focusing.set(properties)
        focusType.set(FocusType.EDIT)
    }

    /**
     * Opens the editor to the item currently being focused
     */
    fun openEditor() {
        openEditor(focusing.get()!!)
    }

    /**
     * Closes focus and restores the view of [stateToRestore]
     */
    fun closeFocus() {
        if (focusType.get() == FocusType.EDIT) {
            focusEditComponent.onClose()
        } else {
            focusListComponent.onClose()
        }

        if (stateToRestore == View.LIST) {
            focusing.set { null }
        } else {
            focusType.set(FocusType.VIEW)
            stateToRestore = View.LIST
        }
    }

    /**
     * On escape button pressed, returns true if call has been handled by method.
     */
    private fun onEscapeButtonPressed(): Boolean {
        if (currentView.get() != View.FOCUS) {
            return false
        }
        if (focusType.get() == FocusType.EDIT) {
            focusEditComponent.onBackButtonPressed()
        } else {
            closeFocus()
        }
        return true
    }

    override fun onScreenClose() {
        super.onScreenClose()

        providerManager.cleanup()
        for (closeOperation in closeOperations) {
            closeOperation()
        }
    }

    fun externalDelete(paths: Set<Path>) {
        providerManager.externalDelete(paths)
        listViewComponent.reload()
        val focused = focusing.get() ?: return
        // Return to list if the item currently being focused is deleted
        if (focused.id is LocalScreenshot && focused.id.path in paths) {
            stateToRestore = View.LIST
            closeFocus()
        } else if (focusType.get() == FocusType.VIEW) {
            // Reload in case one of the images on either side of the focused one
            // was deleted
            openFocusView(focused)
        }
    }


    override fun updateGuiScale() {
        newGuiScale = GuiUtil.getGuiScale()
        super.updateGuiScale()
    }

    override fun onResize(mcIn: Minecraft, w: Int, h: Int) {
        newGuiScale = GuiUtil.getGuiScale()

        super.onResize(mcIn, w, h)
    }

    /**
     * Called by [gg.essential.network.connectionmanager.media.ScreenshotManager]
     * after an edited image has been saved
     */
    fun editCallback(newScreenshot: Path, openFocusView: Boolean) {
        providerManager.reloadItems()
        listViewComponent.reload()
        focusing.get()?.let { focusListComponent.beginPreview(it) }
        if (openFocusView) {
            val properties = providerManager.propertyMap[LocalScreenshot(newScreenshot)]
            if (properties != null) {
                if (focusType.get() == FocusType.EDIT) {
                    closeFocus()
                }
                openFocusView(properties)
            }
        }
    }

    /**
     * Registers an action that is run when the UI is closed
     */
    fun closeOperation(function: () -> Unit) {
        closeOperations.add(function)
    }

    companion object {
        val DATE_FORMAT: DateFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
    }

}
