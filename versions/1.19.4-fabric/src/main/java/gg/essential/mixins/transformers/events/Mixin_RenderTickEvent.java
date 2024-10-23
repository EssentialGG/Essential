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
package gg.essential.mixins.transformers.events;

import gg.essential.Essential;
import gg.essential.event.render.RenderTickEvent;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12100
//$$ import gg.essential.mixins.transformers.client.renderer.DynamicRenderTickCounterAccessor;
//$$ import net.minecraft.client.render.RenderTickCounter;
//#endif

@Mixin(GameRenderer.class)
public class Mixin_RenderTickEvent {

    @Inject(method = "render", at = @At("HEAD"))
    //#if MC>=12100
    //$$ private void renderTickPre(RenderTickCounter tickDelta, boolean tick, CallbackInfo callbackInfo) {
    //#else
    private void renderTickPre(float tickDelta, long startTime, boolean tick, CallbackInfo callbackInfo) {
    //#endif
        fireTickEvent(true, tickDelta);
    }

    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=toasts"))
    //#if MC>=12100
    //$$ private void renderTickPost(RenderTickCounter tickDelta, boolean tick, CallbackInfo callbackInfo) {
    //#else
    private void renderTickPost(float tickDelta, long startTime, boolean tick, CallbackInfo callbackInfo) {
    //#endif
        fireTickEvent(false, tickDelta);
    }

    @Unique
    private void fireTickEvent(
        boolean pre,
        //#if MC>=12100
        //$$ RenderTickCounter counter
        //#else
        float tickDelta
        //#endif
    ) {
        UMatrixStack matrixStack = new UMatrixStack();
        //#if MC>=12100
        //$$ float partialTicksMenu = ((DynamicRenderTickCounterAccessor) counter).essential$getRawTickDelta();
        //$$ float partialTicksInGame = counter.getTickDelta(false);
        //#else
        float partialTicksMenu = UMinecraft.getMinecraft().getTickDelta();
        float partialTicksInGame = tickDelta;
        //#endif
        Essential.EVENT_BUS.post(new RenderTickEvent(pre, false, matrixStack, partialTicksMenu, partialTicksInGame));
    }

}
