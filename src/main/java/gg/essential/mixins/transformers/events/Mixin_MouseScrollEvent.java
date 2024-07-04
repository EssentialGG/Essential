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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.Essential;
import gg.essential.event.gui.MouseScrollEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiScreen.class)
public class Mixin_MouseScrollEvent {
    @ModifyExpressionValue(method = "handleInput", at = @At(value = "NEW", target = "net/minecraftforge/client/event/GuiScreenEvent$MouseInputEvent$Pre"))
    private GuiScreenEvent.MouseInputEvent.Pre onHandleMouseInput(GuiScreenEvent.MouseInputEvent.Pre forgeEvent) {
        int scrollDelta = Mouse.getEventDWheel();
        if (scrollDelta != 0) {
            MouseScrollEvent event = new MouseScrollEvent(scrollDelta, getScreen());
            Essential.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                forgeEvent.setCanceled(true);
            }
        }
        return forgeEvent;
    }

    @Unique
    private GuiScreen getScreen() {
        return (GuiScreen) (Object) this;
    }
}
