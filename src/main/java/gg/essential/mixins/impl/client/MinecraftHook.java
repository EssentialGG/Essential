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
package gg.essential.mixins.impl.client;

import gg.essential.Essential;
import gg.essential.event.client.ClientTickEvent;
import gg.essential.event.client.InitializationEvent;
import gg.essential.event.client.PostInitializationEvent;
import gg.essential.event.client.PreInitializationEvent;
import gg.essential.event.gui.GuiOpenEvent;
import gg.essential.event.network.server.ServerLeaveEvent;
import gg.essential.handlers.ShutdownHook;
import gg.essential.mixins.impl.ClassHook;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.profiler.Profiler;

//#if MC >= 11400
//$$ import net.minecraft.profiler.IProfiler;
//#endif

public class MinecraftHook extends ClassHook<Minecraft> {

    public MinecraftHook(Minecraft instance) {
        super(instance);
    }

    public void preinit() {
        Essential.EVENT_BUS.register(Essential.getInstance());
        Essential.EVENT_BUS.post(new PreInitializationEvent());
    }

    public void startGame() {
        Essential.EVENT_BUS.post(new InitializationEvent());
    }

    public void postInit() {
        Essential.EVENT_BUS.post(new PostInitializationEvent());
    }

    public void runTick() {
        //#if MC>=12102
        //$$ final Profiler mcProfiler = net.minecraft.util.profiler.Profilers.get();
        //#elseif MC < 11400
        final Profiler mcProfiler = UMinecraft.getMinecraft().mcProfiler;
        //#else
        //$$ final IProfiler mcProfiler = UMinecraft.getMinecraft().getProfiler();
        //#endif
        mcProfiler.startSection("essential_tick");
        ClientTickEvent.counter++;
        Essential.EVENT_BUS.post(new ClientTickEvent());
        mcProfiler.endSection();
    }

    public void disconnect() {
        Essential.EVENT_BUS.post(new ServerLeaveEvent());
    }

    public void shutdown() {
        ShutdownHook.INSTANCE.execute();
    }

    public GuiOpenEvent displayGuiScreen(GuiScreen screen) {
        GuiOpenEvent guiOpenEvent = new GuiOpenEvent(screen);
        Essential.EVENT_BUS.post(guiOpenEvent);
        return guiOpenEvent;
    }
}
