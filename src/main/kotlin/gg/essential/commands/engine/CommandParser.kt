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
package gg.essential.commands.engine

import gg.essential.Essential
import gg.essential.universal.ChatColor
import gg.essential.util.*
import gg.essential.api.commands.*
import gg.essential.commands.api.ArgumentQueueImpl
import gg.essential.commands.api.WhitespaceSensitiveArgumentQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

object CommandParser {
    private val argumentParsers = mutableMapOf<Class<*>, ArgumentParser<*>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Client)

    init {
        registerArgumentParser(Int::class.javaPrimitiveType!!, IntArgumentParser())
        registerArgumentParser(Int::class.javaObjectType, IntArgumentParser())

        registerArgumentParser(Boolean::class.javaPrimitiveType!!, BooleanArgumentParser())
        registerArgumentParser(Boolean::class.javaObjectType, BooleanArgumentParser())

        registerArgumentParser(Double::class.javaPrimitiveType!!, DoubleArgumentParser())
        registerArgumentParser(Double::class.javaObjectType, DoubleArgumentParser())

        registerArgumentParser(Float::class.javaPrimitiveType!!, FloatArgumentParser())
        registerArgumentParser(Float::class.javaObjectType, FloatArgumentParser())

        registerArgumentParser(StringArgumentParser())
        registerArgumentParser(PlayerArgumentParser())
        registerArgumentParser(EssentialFriendArgumentParser())
        registerArgumentParser(EssentialUserArgumentParser)
    }

    inline fun <reified T> registerArgumentParser(parser: ArgumentParser<T>) {
        registerArgumentParser(T::class.java, parser)
    }

    fun <T> registerArgumentParser(type: Class<T>, parser: ArgumentParser<T>) {
        argumentParsers[type] = parser
    }

    internal fun parseCommandAndCallHandler(arguments: List<String>, handler: Command.Handler, command: Command) {
        val queue = ArgumentQueueImpl(LinkedList(arguments))

        val parsedParams = handler.params { param, kParam ->
            val parsed = parseParameter(param, kParam, queue)

            when (parsed) {
                is Success -> queue.sync()
                is Failure -> {
                    queue.undo()
                    // TODO: Print argument specific usage info?
                    MinecraftUtils.sendMessage("", "${ChatColor.RED}Usage: ${getHandlerUsage(command, handler)}")
                    return
                }
            }

            parsed.asSuccess.value
        }.toTypedArray()

        if (!queue.isEmpty()) {
            MinecraftUtils.sendMessage("", "${ChatColor.RED}Usage: ${getHandlerUsage(command, handler)}")
            return
        }

        val kotlinFunction = handler.method.kotlinFunction
        if (kotlinFunction != null && kotlinFunction.isSuspend) {
            coroutineScope.launch {
                try {
                    kotlinFunction.callSuspend(command, *parsedParams)
                } catch (e: Throwable) {
                    Essential.logger.error("Error executing command handler:", e)
                    MinecraftUtils.sendMessage("", "${ChatColor.RED}Unhandled error: ${e.cause}")
                }
            }
        } else {
            handler.method.invoke(command, *parsedParams)
        }
    }

    internal fun getHandlerUsage(command: Command, handler: Command.Handler): String {
        val builder = StringBuilder()

        builder.append("/").append(command.name)

        if (handler.method.isAnnotationPresent(SubCommand::class.java)) {
            val subCommandName = handler.method.getAnnotation(SubCommand::class.java).value
            builder.append(" ").append(subCommandName)
        }

        handler.params { param, kParam ->
            val isOptional = param.type == Optional::class.java ||
                (kParam != null && kParam.type.isMarkedNullable)

            val paramContent = when {
                param.isAnnotationPresent(Options::class.java) -> {
                    param.getAnnotation(Options::class.java).value.sorted().joinToString(separator = "|")
                }
                param.isAnnotationPresent(DisplayName::class.java) -> {
                    param.getAnnotation(DisplayName::class.java).value
                }
                else -> {
                    kParam?.name ?: param.name
                }
            }

            builder.append(" ")
                .append(if (isOptional) "[" else "<")
                .append(paramContent)
                .append(if (isOptional) "]" else ">")
        }

        return builder.toString()
    }

    internal fun getCompletionOptions(arguments: List<String>, handler: Command.Handler): List<String> {
        val queue = ArgumentQueueImpl(LinkedList(arguments))

        handler.params { param, kParam ->
            val parsed = parseParameter(param, kParam, queue)

            if (queue.isEmpty()) {
                queue.undo()
                val (type) = collectTypeInformation(param, kParam)

                val parser = argumentParsers[type] ?: return emptyList()

                return Try { parser.complete(queue, param) }.recover(emptyList()).value.sorted()
            }

            when (parsed) {
                is Success -> queue.sync()
                is Failure -> queue.undo()
            }
        }

        return emptyList()
    }

    internal inline fun <T> Command.Handler.params(func: (Parameter, KParameter?) -> T): List<T> {
        return params.mapIndexed { index, param ->
            val kParam = kParams?.get(index)

            func(param, kParam)
        }
    }

    internal fun parseParameter(param: Parameter, kParam: KParameter?, queue: ArgumentQueue): Try<Any?> {
        val (type, isJavaOptional, isNullable) = collectTypeInformation(param, kParam)

        val result = getParsedArgument(param, type, queue)

        if (isJavaOptional) {
            return result.force(Optional.empty()) { Optional.of(it) }
        } else if (isNullable) {
            return result.recover(null)
        }

        return result
    }

    internal fun collectTypeInformation(param: Parameter, kParam: KParameter?): Triple<Class<*>, Boolean, Boolean> {
        var type = param.type

        val isJavaOptional = type == Optional::class.java
        val isNullable = (kParam != null && kParam.type.isMarkedNullable)

        if (isJavaOptional) {
            type = ((param.parameterizedType as ParameterizedType).actualTypeArguments[0] as Class<*>)
        }

        return Triple(type, isJavaOptional, isNullable)
    }

    private fun getParsedArgument(param: Parameter, type: Class<*>, queue: ArgumentQueue): Try<Any> {
        // TODO: Perhaps allow multiple parsers for a single type, each with a priority, trying to parse using them
        //  until one succeeds?
        val parser = argumentParsers[type] ?: return Failure

        return Try {
            parser.parse(queue, param)!!.also { (queue as? WhitespaceSensitiveArgumentQueue)?.markEndOfArgument() }
        }
    }
}
