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
/**
 * Minecraft sets the current server data in the constructor of GuiConnecting but only resets it if the world is
 * unloaded. Consequently, if you get disconnected before the world is loaded, or you abort the connection attempt, then
 * it will not be reset and various code which uses it to determine whether we are on a server will behave incorrectly.
 */
package gg.essential.mixins.transformers.compatibility.vanilla.serverdatareset;