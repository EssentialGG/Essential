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
package gg.essential.util.lwjgl3.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import java.util.function.BiFunction

/**
 * Redirect classes from the actual GL classes to the GLBridge (which bridges to the GL outside the isolated loader).
 */
class GLBridgeTransformer : BiFunction<String, ByteArray, ByteArray> {
    override fun apply(name: String, bytes: ByteArray): ByteArray = when (name) {
        "org.lwjgl.nanovg.NanoVGGLConfig" -> {
            val node = ClassNode().apply { ClassReader(bytes).accept(this, 0) }
            node.methods
                .first { it.name == "configGL" }
                .instructions
                .toArray()
                .filterIsInstance<LdcInsnNode>()
                .first { it.cst == "org.lwjgl.opengl.GL" }
                .cst = "gg.essential.util.lwjgl3.impl.GLBridge"
            ClassWriter(0).apply { node.accept(this) }.toByteArray()
        }
        else -> bytes
    }
}