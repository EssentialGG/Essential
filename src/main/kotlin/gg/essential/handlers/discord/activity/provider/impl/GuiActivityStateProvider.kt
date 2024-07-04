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
package gg.essential.handlers.discord.activity.provider.impl

import gg.essential.Essential
import gg.essential.api.gui.EssentialGUI
import gg.essential.handlers.discord.activity.ActivityState
import gg.essential.handlers.discord.activity.provider.ActivityStateProvider
import gg.essential.universal.UMinecraft
import gg.essential.util.isMainMenu
import gg.essential.vigilance.gui.SettingsGui
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiScreenAddServer
import net.minecraft.client.gui.GuiScreenResourcePacks
import net.minecraft.client.gui.GuiScreenServerList

//#if FORGE
//#if MC<11700
typealias GuiModList = net.minecraftforge.fml.client.GuiModList
//#elseif MC<11802
//$$ typealias GuiModList = net.minecraftforge.fmlclient.gui.screen.ModListScreen
//#else
//$$ typealias GuiModList = net.minecraftforge.client.gui.ModListScreen
//#endif
//#endif

/**
 * Shows the user's currently opened GUI on their presence via an [ActivityState.GUI] object
 */
class GuiActivityStateProvider : ActivityStateProvider {
    override fun init() {
        Essential.getInstance().registerListener(this)
    }

    override fun provide(): ActivityState? {
        val screen = UMinecraft.getMinecraft().currentScreen ?: return null
        return stateForScreen(screen)
    }

    private fun stateForScreen(screen: GuiScreen): ActivityState? {
        // We don't have access to these classes at compile time, so let's just check if the name matches
        when (screen.javaClass.name) {
            "net.labymod.gui.ModGuiMultiplayer" -> return ActivityState.GUI.ServerList
        }

        // These are screens on our classpath, we can reference them like this instead of their java class name
        // NOTE: We are not using an import when interacting with the preprocessor as it could get messed up when
        //       formatting this class.
        return when (screen) {
            is EssentialGUI -> screen.discordActivityDescription?.let { ActivityState.GUI.Described(it) }
            is GuiScreenAddServer, is GuiMultiplayer -> ActivityState.GUI.ServerList
            is GuiOptions, is GuiScreenResourcePacks -> ActivityState.GUI.Options()
            is GuiScreenServerList -> ActivityState.GUI.ServerList
            //#if MC<11300
            // NOTE: Snooper was removed in 1.13 because of GDPR.
            is net.minecraft.client.gui.GuiSnooper -> ActivityState.GUI.Options()
            //#endif
            //#if MC<11600
            is net.minecraft.client.gui.GuiCustomizeSkin,
            is net.minecraft.client.gui.GuiControls,
            is net.minecraft.client.gui.GuiLanguage,
            is net.minecraft.client.gui.ScreenChatOptions,
            is net.minecraft.client.gui.GuiScreenOptionsSounds -> ActivityState.GUI.Options()
            //#else
            //$$ is net.minecraft.client.gui.screen.SettingsScreen -> ActivityState.GUI.Options()
            //#endif
            is SettingsGui -> ActivityState.GUI.Options(false)
            //#if FORGE
            is GuiModList -> ActivityState.GUI.Options(false)
            //#endif
            else -> {
                if (screen.isMainMenu) {
                    ActivityState.GUI.MainMenu
                } else {
                    null
                }
            }
        }
    }
}