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
package gg.essential.util

import gg.essential.elementa.components.Window
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.overlay.ModalManager
import gg.essential.model.backend.RenderBackend
import gg.essential.network.CMConnection
import gg.essential.universal.UImage
import gg.essential.universal.utils.ReleasedDynamicTexture
import gg.essential.util.image.bitmap.MutableBitmap
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.jvm.Throws

interface GuiEssentialPlatform {
    val clientThreadDispatcher: CoroutineDispatcher
    val renderThreadDispatcher: CoroutineDispatcher

    val renderBackend: RenderBackend

    val cmConnection: CMConnection

    fun createModalManager(): ModalManager

    fun onResourceManagerReload(runnable: Runnable)

    @Throws(IOException::class)
    fun bitmapFromMinecraftResource(identifier: UIdentifier): MutableBitmap?

    @Throws(IOException::class)
    fun bitmapFromInputStream(inputStream: InputStream): MutableBitmap

    fun uImageIntoReleasedDynamicTexture(uImage: UImage): ReleasedDynamicTexture

    fun registerCosmeticTexture(name: String, texture: ReleasedDynamicTexture): UIdentifier

    fun dismissModalOnScreenChange(modal: Modal, dismiss: () -> Unit)

    val essentialBaseDir: Path
    val config: Config
    val pauseMenuDisplayWindow: Window

    fun currentServerType(): ServerType?

    fun registerActiveSessionState(state: MutableState<USession>)

    interface Config {
        val shouldDarkenRetexturedButtons: Boolean
        val useVanillaButtonForRetexturing: State<Boolean>
    }

    companion object {
        internal val platform: GuiEssentialPlatform =
            Class.forName(GuiEssentialPlatform::class.java.name + "Impl").newInstance() as GuiEssentialPlatform
    }
}
