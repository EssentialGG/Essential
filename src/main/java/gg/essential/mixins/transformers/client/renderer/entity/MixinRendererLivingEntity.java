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
package gg.essential.mixins.transformers.client.renderer.entity;

import gg.essential.gui.common.UI3DPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC<=10809
//$$ import net.minecraft.client.renderer.entity.RendererLivingEntity;
//#else
import net.minecraft.client.renderer.entity.RenderLivingBase;
//#endif

//#if MC>=12102
//$$ import net.minecraft.client.render.entity.state.LivingEntityRenderState;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.render.entity.EntityRendererFactory;
//#endif

//#if MC<=10809
//$$ @Mixin(value = RendererLivingEntity.class, priority = 500)
//#else
@Mixin(value = RenderLivingBase.class, priority = 500)
//#endif

//#if MC>=12102
//$$ public abstract class MixinRendererLivingEntity<T extends LivingEntity, S extends LivingEntityRenderState> extends EntityRenderer<T, S> {
//#else
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {
//#endif
    protected MixinRendererLivingEntity() {
        super(null);
    }

    //#if MC>=12102
    //$$ @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    //$$ private void canRenderNameOfEmulatedPlayer(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> ci) {
    //#else
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void canRenderNameOfEmulatedPlayer(T entity, CallbackInfoReturnable<Boolean> ci) {
    //#endif
        UI3DPlayer component = UI3DPlayer.current;
        if (component != null) {
            ci.setReturnValue(!component.getHideNameTags().get());
            return;
        }

    }

    //#if MC>=11600
    //$$ // Does not apply. Lighting state is now passed via arguments and therefore well scoped.
    //#else
    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V", shift = At.Shift.AFTER))
    private void restoreMenuLighting(CallbackInfo ci) {
        UI3DPlayer component = UI3DPlayer.current;
        if (component != null) {
            GlStateManager.disableTexture2D();
        }
    }
    //#endif
}