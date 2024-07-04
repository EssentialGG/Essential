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
package gg.essential.gui.overlay

/**
 * A layer which will automatically be disposed of when it no longer has any content.
 */
class EphemeralLayer(priority: LayerPriority) : Layer(priority) {
    /**
     * Callback which will be called before the layer is disposed.
     * Re-adding content to the layer from within this callback will prevent it from being disposed until it is empty
     * again, at which point the callback will be invoked again.
     */
    var onClose: () -> Unit = {}
}