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
package gg.essential.mixins.transformers.compatibility.vanilla;

import gg.essential.universal.shader.BlendState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft's GlBlendState is fundamentally broken because it does not update anything if the previously active blend
 * state matches the to-be-applied one. But that assumes that it is the only method which can modify the global GL state,
 * which is not a good assumption, and MC itself immediately violates it in RenderLayer.
 * Also, the function itself is broken too, because it only updates the last-active state when the enabled state
 * changes but not when any of the equations change.
 * As such, it's basically a gamble how much of the global gl state that method actually updates any time you call it.
 * In many places it's routinely called but doesn't do anything.
 * What makes it tricky for us is that various parts of MC (and potentially third-party mods) have even come to rely on
 * this broken behavior, making it infeasible to fix directly.
 * <br>
 * One of the bugs that's hidden by it is that the final framebuffer-to-backbuffer copy should have blending disabled
 * (because it's supposed to just copy) but Mojang didn't notice that after the Framebuffer.draw method explicitly
 * disables blending, it invokes ShaderProgram.bind which will use the broken GlBlendState to re-enable blending (because
 * it's enabled for the blit shader; questionable decision but changing that would likely break even more because it's
 * used in various post-processing shaders); and they didn't notice this because GlBlendState is broken and doesn't
 * actually activate it depending on where and how it was last used.
 * As such, sometimes one can end up with a red tint on anything semi-transparent after the blit:
 * E.g. <a href="https://cdn.discordapp.com/attachments/887708890127536128/1123258266147885136/image.png">EM-1826</a>
 * <br>
 * <br>
 * Directly fixing this bug may too be risky because other parts of the game (or third-party mods) may very well have
 * come to rely on that behavior (which as mentioned could be consistently different depending on where draw is called).
 * So instead this mixin works around that by intentionally re-creating the state, in which GlBlendState does nothing,
 * for this particular framebuffer blit only.
 * To do this, we must enable a GlBlendState that has the same settings as the blit shader (which happens to be the
 * `NORMAL` ones). MC can then explicitly disable bending and once it enables the shader, GlBlendState won't revert that
 * because as far as it is concerned, everything is already up-to-date. Due to an aforementioned bug in GlBlendState's
 * implementation itself, where it only saves the new state if the enabled flag changes, we must first however disable
 * blending before enabling the normal one, as otherwise the normal one might not be saved.
 */
@Mixin(MinecraftClient.class)
public abstract class Mixin_WorkaroundBrokenFramebufferBlitBlending {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"))
    private void disableBlendingForBlit(CallbackInfo ci) {
        BlendState.DISABLED.activate();
        BlendState.NORMAL.activate();
    }
}
