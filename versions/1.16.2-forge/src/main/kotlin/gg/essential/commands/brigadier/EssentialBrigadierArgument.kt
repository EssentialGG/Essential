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
package gg.essential.commands.brigadier

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import gg.essential.commands.EssentialCommandRegistry
import gg.essential.commands.engine.CommandParser
import gg.essential.util.textLiteral
import java.lang.reflect.Parameter
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KParameter

class EssentialBrigadierArgument(private val parameter: Parameter, private val kParameter: KParameter?) : ArgumentType<Any?> {
    override fun parse(reader: StringReader): Any? {
        return CommandParser.parseParameter(parameter, kParameter, StringReaderArgumentQueue(reader)).resolve {
            val message = textLiteral("Invalid argument for type ${parameter.type.simpleName}")
            throw CommandSyntaxException(SimpleCommandExceptionType(message), message, reader.string, reader.cursor)
        }
    }

    override fun <S : Any?> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        EssentialCommandRegistry.getCompletionOptions(builder.input).forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}