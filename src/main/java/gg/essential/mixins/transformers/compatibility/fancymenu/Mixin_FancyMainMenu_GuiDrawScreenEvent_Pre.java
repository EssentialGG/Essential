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
package gg.essential.mixins.transformers.compatibility.fancymenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FancyMenu cancels the regular rendering of the Main Menu when customizations are enabled.
 * We still want to draw our main menu stuff though, so this mixin fires our pre event from Fancy Menu's handler.
 * This mixin handles FancyMenu versions 2.14.9 and below on Fabric, and all versions on Forge.
 *
 * @see Mixin_FancyMainMenu_2_14_10_GuiDrawScreenEvent_Pre
 */
@Pseudo
@Mixin(targets = "de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler", remap = false)
public class Mixin_FancyMainMenu_GuiDrawScreenEvent_Pre extends Mixin_FancyMainMenu_GuiDrawScreenEvent_Post {
    //#if FABRIC
    //$$ private static final String TARGET = "Lde/keksuccino/konkrete/events/client/GuiScreenEvent$DrawScreenEvent$Pre;setCanceled(Z)V";
    //#elseif MC>=11900
    //$$ private static final String TARGET = "Lnet/minecraftforge/client/event/ScreenEvent$Render$Pre;setCanceled(Z)V";
    //#elseif MC>=11800
    //$$ private static final String TARGET = "Lnet/minecraftforge/client/event/ScreenEvent$DrawScreenEvent$Pre;setCanceled(Z)V";
    //#else
    private static final String TARGET = "Lnet/minecraftforge/client/event/GuiScreenEvent$DrawScreenEvent$Pre;setCanceled(Z)V";
    //#endif

    // https://github.com/Keksuccino/FancyMenu/blob/7da1f5f8a3b16d10b7359cd53bbbac940860a6f7/src/main/java/de/keksuccino/fancymenu/menu/fancy/menuhandler/custom/MainMenuHandler.java#L117-L125
    @Inject(method = "onRender", at = @At(value = "INVOKE", target = TARGET))
    private void emitEssentialPreEvent(@Coerce KonkreteDrawScreenEventAcc event, CallbackInfo ci) {
        emitPostEvent = true;

        emitEssentialEvent(event, false);
    }
}
