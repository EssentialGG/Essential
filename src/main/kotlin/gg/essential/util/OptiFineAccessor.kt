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
package gg.essential.util

import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

@Suppress("PrivatePropertyName", "FunctionName")
class OptiFineAccessor {
    private val lookup = MethodHandles.lookup()

    //#if MC>=11400
    //$$ private val Config = Class.forName("net.optifine.Config")
    //#else
    private val Config = Class.forName("Config")
    //#endif
    private val Shaders = Class.forName("net.optifine.shaders.Shaders")

    private val Config_isShaders = lookup.unreflect(Config.getDeclaredMethod("isShaders"))
    fun Config_isShaders(): Boolean = Config_isShaders.invokeExact() as Boolean

    private val Shaders_pushProgram = lookup.unreflect(Shaders.getDeclaredMethod("pushProgram"))
    private val Shaders_popProgram = lookup.unreflect(Shaders.getDeclaredMethod("popProgram"))
    fun Shaders_pushProgram() { Shaders_pushProgram.invokeExact() }
    fun Shaders_popProgram() { Shaders_popProgram.invokeExact() }

    private val Shaders_beginSpiderEyes = lookup.unreflect(Shaders.getDeclaredMethod("beginSpiderEyes"))
    private val Shaders_endSpiderEyes = lookup.unreflect(Shaders.getDeclaredMethod("endSpiderEyes"))
    fun Shaders_beginSpiderEyes() { Shaders_beginSpiderEyes.invokeExact() }
    fun Shaders_endSpiderEyes() { Shaders_endSpiderEyes.invokeExact() }

    companion object {
        private val logger = LoggerFactory.getLogger(OptiFineAccessor::class.java)

        val INSTANCE by lazy {
            if (OptiFineUtil.isLoaded()) {
                try {
                    OptiFineAccessor()
                } catch (e: Exception) {
                    logger.error("Failed to initialize reflective OptiFine accessor:", e)
                    null
                }
            } else {
                null
            }
        }
    }
}