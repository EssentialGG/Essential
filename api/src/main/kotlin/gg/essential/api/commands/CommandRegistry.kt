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
package gg.essential.api.commands

/**
 * This is where you must register all of the [Command] instances you create so Essential can provide them to
 * the user.
 */
interface CommandRegistry {
    /**
     * Add your command instance to the command registry, making it available for use.
     */
    fun registerCommand(command: Command)

    /**
     * If one of your command handlers wishes to take a custom type, you must provide a way for the command engine
     * to turn an [ArgumentQueue] into your instance. This is done via an implementation of [ArgumentParser].
     *
     * There is no need to create custom parsers for default types such as: strings, integers, doubles, and booleans.
     */
    fun <T> registerParser(type: Class<T>, parser: ArgumentParser<T>)

    /**
     * Remove your command from the command registry.
     */
    fun unregisterCommand(command: Command)
}
