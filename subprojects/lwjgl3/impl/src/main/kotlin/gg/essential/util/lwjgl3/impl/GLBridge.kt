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
package gg.essential.util.lwjgl3.impl

import gg.essential.config.AccessedViaReflection
import org.lwjgl.system.FunctionProvider
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Bridges NanoVG to the outer OpenGL context.
 */
@AccessedViaReflection("GLBridgeTransformer")
@Suppress("unused") // called from NanoVGGLConfig via asm injected by GLBridgeTransformer
object GLBridge {
    /** We are inside of an isolated class loader, this is the outer/regular class loader. */
    private val outerClassLoader = javaClass.classLoader.javaClass.classLoader

    @AccessedViaReflection("GLBridgeTransformer")
    @JvmStatic
    fun getCapabilities() {} // just an extra check, we can no-op it

    @AccessedViaReflection("GLBridgeTransformer")
    @JvmStatic
    fun getFunctionProvider(): FunctionProvider {
        return try {
            // LWJGL2
            val method = Class.forName("org.lwjgl.opengl.GLContext", true, outerClassLoader)
                .getDeclaredMethod("ngetFunctionAddress", Long::class.java)
                .apply { isAccessible = true }
            FunctionProvider { method.invoke(null, MemoryUtil.memAddress(it)) as Long }
        } catch (e: ClassNotFoundException) {
            // LWJGL3
            val outerProvider = Class.forName("org.lwjgl.opengl.GL", true, outerClassLoader)
                .getDeclaredMethod("getFunctionProvider")
                .invoke(null)
            val method = Class.forName(FunctionProvider::class.java.name, true, outerClassLoader)
                .getMethod("getFunctionAddress", ByteBuffer::class.java)
            return FunctionProvider { method.invoke(outerProvider, it) as Long }
        }
    }
}