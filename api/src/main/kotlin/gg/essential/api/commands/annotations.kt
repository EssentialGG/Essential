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
 * Marks the annotated function as a subcommand of the [Command]
 * the function resides in.
 *
 * This is mostly a shortcut for using the [Options] annotation + a switch, although note it is only
 * for the first sub-level of command.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SubCommand(val value: String, val aliases: Array<String> = [], val description: String = "")

/**
 * Marks the annotated function as the default handler of the [Command]
 * the function resides in. That means if the Command has no [SubCommand]s, or the user didn't specify one of those
 * subcommands, this function will be invoked instead.
 *
 * If no DefaultHandler is specified, the Command's usage will be printed instead.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultHandler

/**
 * Provides a custom display name for this parameter. This is used when printing a command's usage.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisplayName(val value: String)

/**
 * Only allows the given list of strings to be passed to the annotated argument.
 *
 * This is most useful for nested subcommands.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Options(val value: Array<String>)

/**
 * Marks this parameter as greedy and should take up the rest of the available command.
 * This is primarily meant for the last parameter in a function, as anything after it will be left without
 * a value.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Greedy

/**
 * Indicates that this parameter should take [value] words of the command.
 *
 * If [allowLess] is set to true, anything from 1 to [value] words are allowed,
 * however if it is set to false an error will be thrown and the usage message
 * will be displayed, which should indicate to the user the amount of words
 * needed.
 *
 * If you need this specific amount of words to create a custom data type
 * look into creating and registering an [ArgumentParser] instead.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Take(val value: Int, val allowLess: Boolean = true)

/**
 * Indicates that this string parameter can be quoted.
 *
 * This means that if the arguments passed in are
 * /command "one two three" four
 * and the first accepted parameter is [Quotable] then it will receive
 * the string 'one two three'.
 *
 * If no quotes are present, then only the first word will be passed in.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Quotable
