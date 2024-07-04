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
// 1.12.2 and below
package gg.essential.mixins.transformers.events;

import gg.essential.Essential;
import gg.essential.event.gui.GuiClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiScreen.class)
public abstract class Mixin_GuiClickEvent_Priority {

    @Shadow public Minecraft mc;
    @Shadow public int width;
    @Shadow public int height;

    @ModifyArg(method = "handleInput", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", remap = false))
    private Event priorityEvents(Event forgeEvent) {
        if (forgeEvent instanceof GuiScreenEvent.MouseInputEvent.Pre && Mouse.getEventButtonState()) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            int eventButton = Mouse.getEventButton();

            GuiClickEvent.Priority event = new GuiClickEvent.Priority(mouseX, mouseY, eventButton, (GuiScreen) (Object) this);
            Essential.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                forgeEvent.setCanceled(true);
            }
        }
        return forgeEvent;
    }
}
