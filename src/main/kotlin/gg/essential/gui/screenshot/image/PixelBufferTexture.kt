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
package gg.essential.gui.screenshot.image

import gg.essential.gui.screenshot.downsampling.ErrorImage
import gg.essential.gui.screenshot.downsampling.PixelBuffer
import gg.essential.universal.UMinecraft
import net.minecraft.client.renderer.GlStateManager

import net.minecraft.client.resources.IResourceManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.nio.IntBuffer

//#if MC<=11202
import net.minecraft.client.renderer.texture.AbstractTexture
//#else
//$$ import net.minecraft.client.renderer.texture.Texture
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.platform.GlStateManager
//#endif

/**
 * Uploads the contents of a PixelBuffer to OpenGL
 */
class PixelBufferTexture(image: PixelBuffer) :
//#if MC<=11202
    AbstractTexture() {
    //#else
    //$$ Texture() {
    //#endif

    // Whether this texture's underlying image had an error during loading
    // To be used in ScreenshotBrowser for alternate behavior
    val error = image is ErrorImage

    val imageWidth: Int = image.getWidth()
    val imageHeight: Int = image.getHeight()

    init {

        if(image !is ErrorImage) {
            glTextureId = GL11.glGenTextures()

            // We need to support both running on the main thread and running in another async context
            if (UMinecraft.getMinecraft().isCallingFromMinecraftThread) {
                GlStateManager.bindTexture(glTextureId)
                // Minecraft changes these values in some places, for example NativeImage#upload
                glPixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0)
                glPixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0)
                glPixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0)
            } else {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId)
            }

            val buffer = image.prepareDirectBuffer()
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                imageWidth,
                imageHeight,
                0,
                GL11.GL_RGBA,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                null as IntBuffer?
            )

            // Extra GL call should not be required but is because of a suspected graphics driver bug
            // This acts as a workaround fix for the texture not uploading and rendering correctly
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                GL11.GL_RGBA,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                buffer
            )
        }
    }

    //#if MC<12104
    //Impl handled by constructor to avoid retaining complete image
    override fun loadTexture(resourceManager: IResourceManager) {
    }
    //#endif


}

fun glPixelStore(pname: Int, param: Int) {
    // FIXME remap bug: Doesn't seem to be a way to do this with mapping overrides, since GlStateManager is overridden to RenderSystem
    //#if MC>=11600
    //$$ GlStateManager.pixelStore(pname, param)
    //#elseif MC>=11200
    GlStateManager.glPixelStorei(pname, param)
    //#else
    //$$ GL11.glPixelStorei(pname, param)
    //#endif
}
