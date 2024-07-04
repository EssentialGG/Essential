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

import java.lang.reflect.Parameter

/**
 * Defines how to parse a certain data type using the available command parameters.
 *
 * Register using a [CommandRegistry]
 */
interface ArgumentParser <T> {
    /**
     * Parses to a certain type based on the arguments provided by the user
     * and the parameter (for accessing annotations).
     *
     * If the arguments provided do not allow for your custom type to be created, throw an
     * Exception, or return null.
     */
    @Throws(Exception::class)
    fun parse(arguments: ArgumentQueue, param: Parameter): T?

    /**
     * Allows this ArgumentParser to provide custom tab completion options.
     *
     * This does not need to be overridden: by default no tab completion options will be available.
     */
    fun complete(arguments: ArgumentQueue, param: Parameter): List<String> = emptyList()
}
