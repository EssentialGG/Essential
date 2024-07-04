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
package gg.essential.mixins.transformers.client.shader;

import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11600
//#else
import net.minecraft.client.renderer.OpenGlHelper;
//#endif

// Note: This mixin is disabled via our mixin plugin for Forge because Forge already includes a similar patch.
//       We do not disable it via preprocessor, so it gets remapped across versions.
// TODO 1.16
@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {
    //#if MC<11600
    @Shadow public abstract void createBindFramebuffer(int width, int height);
    @Shadow public int framebufferWidth;
    @Shadow public int framebufferHeight;
    @Shadow public int depthBuffer;

    private boolean stencilEnabled = false;

    @SuppressWarnings("unused") // Used by Elementa, do not change signature
    public boolean enableStencil() {
        stencilEnabled = true;
        this.createBindFramebuffer(framebufferWidth, framebufferHeight);
        return OpenGlHelper.isFramebufferEnabled();
    }

    @SuppressWarnings("unused") // Used by Elementa, do not change signature
    public boolean isStencilEnabled() {
        return stencilEnabled;
    }

    @ModifyConstant(method = "createFramebuffer", constant = @Constant(intValue = 33190))
    private int getFramebufferFormat(int org) {
        return stencilEnabled ? EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT : org;
    }

    @Redirect(method = "createFramebuffer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/OpenGlHelper;GL_DEPTH_ATTACHMENT:I"))
    private int getDepthAttachmentAndAttachStencil() {
        if (!stencilEnabled) {
            return OpenGlHelper.GL_DEPTH_ATTACHMENT;
        }
        OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
        return EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT;
    }
    //#else
    //$$ @SuppressWarnings("unused") // Used by Elementa, do not change signature
    //$$ public boolean enableStencil() {
    //$$     return false;
    //$$ }
    //$$
    //$$ @SuppressWarnings("unused") // Used by Elementa, do not change signature
    //$$ public boolean isStencilEnabled() {
    //$$     return false;
    //$$ }
    //#endif
}
