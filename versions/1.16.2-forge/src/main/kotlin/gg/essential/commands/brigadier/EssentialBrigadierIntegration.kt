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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import gg.essential.api.commands.Command
import gg.essential.api.commands.DisplayName
import gg.essential.commands.EssentialCommandRegistry
import gg.essential.commands.engine.CommandParser
import gg.essential.commands.engine.CommandParser.params
import gg.essential.util.textLiteral
import net.minecraft.client.multiplayer.ClientSuggestionProvider

object EssentialBrigadierIntegration {
    fun addCommands(target: CommandDispatcher<ClientSuggestionProvider>) {
        for ((name, entry) in EssentialCommandRegistry.commands) {
            val (command, hideFromAutocomplete) = entry
            if (!hideFromAutocomplete) {
                target.register(command.toBrigadierBuilder(name))
            }
        }
    }

    private fun Command.toBrigadierBuilder(name: String): LiteralArgumentBuilder<ClientSuggestionProvider> {
        val result = defaultHandler?.toBrigadierBuilder(name) ?: LiteralArgumentBuilder.literal(name)
        for ((subCommandName, subCommandHandler) in subCommands) {
            result.then(subCommandHandler.toBrigadierBuilder(subCommandName))
        }
        return result
    }

    private fun Command.Handler.toBrigadierBuilder(name: String): LiteralArgumentBuilder<ClientSuggestionProvider> {
        val result = LiteralArgumentBuilder.literal<ClientSuggestionProvider>(name)
        var endOfTree: ArgumentBuilder<ClientSuggestionProvider, *> = result

        val arguments: List<ArgumentBuilder<ClientSuggestionProvider, *>> = listOf(result) + this.params { param, kParam ->
            val displayName = param.getAnnotation(DisplayName::class.java)?.value ?: kParam?.name ?: param.name
            val (_, isJavaOptional, isNullable) = CommandParser.collectTypeInformation(param, kParam)
            if (isJavaOptional || isNullable) {
                // Brigadier determines whether an argument is optional based on whether there is a default handler.
                endOfTree.executes(DummyCommand)
            }

            RequiredArgumentBuilder
                .argument<ClientSuggestionProvider, Any?>(displayName, EssentialBrigadierArgument(param, kParam))
                .also { endOfTree = it }
        }


        arguments.reduceRight { acc, argumentBuilder ->
            @Suppress("UNCHECKED_CAST")
            acc.then(argumentBuilder) as ArgumentBuilder<ClientSuggestionProvider, *>
        }
        return result
    }

    // Our commands are not actually executed through brigadier, this is only used to make it think an argument is optional.
    private object DummyCommand : com.mojang.brigadier.Command<ClientSuggestionProvider> {
        override fun run(context: CommandContext<ClientSuggestionProvider>?): Int {
            val message = textLiteral("Essential's dummy command should not have been executed!")
            throw CommandSyntaxException(SimpleCommandExceptionType(message), message)
        }
    }
}