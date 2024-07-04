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
package gg.essential.mixins.ext.client.renderer

import gg.essential.universal.UImage
import net.minecraft.client.renderer.ThreadDownloadImageData

interface PlayerSkinTextureExt {
    fun `essential$getImage`(): UImage?
    fun `essential$setImage`(image: UImage?)
}

val ThreadDownloadImageData.ext get() = this as PlayerSkinTextureExt
var PlayerSkinTextureExt.image
    get() = `essential$getImage`()
    set(value) = `essential$setImage`(value)
