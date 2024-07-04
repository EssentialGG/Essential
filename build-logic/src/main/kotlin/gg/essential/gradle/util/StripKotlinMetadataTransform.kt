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
package gg.essential.gradle.util

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.Closeable
import java.io.File
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

abstract class StripKotlinMetadataTransform : TransformAction<StripKotlinMetadataTransform.Parameters> {
    interface Parameters : TransformParameters

    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val output = outputs.file(input.nameWithoutExtension + "-without-kotlin-metadata.jar")
        (input to output).useInOut { jarIn, jarOut ->
            while (true) {
                val entry = jarIn.nextJarEntry ?: break
                val originalBytes = jarIn.readBytes()

                val modifiedBytes = if (entry.name.endsWith(".class")) {
                    val reader = ClassReader(originalBytes)
                    val writer = ClassWriter(reader, 0)
                    reader.accept(object : ClassVisitor(Opcodes.ASM9, writer) {
                        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                            if (descriptor == "Lkotlin/Metadata;") {
                                return null
                            }
                            return super.visitAnnotation(descriptor, visible)
                        }
                    }, 0)
                    writer.toByteArray()
                } else {
                    originalBytes
                }

                jarOut.putNextEntry(ZipEntry(entry.name))
                jarOut.write(modifiedBytes)
                jarOut.closeEntry()
            }
        }
    }

    private inline fun Pair<File, File>.useInOut(block: (jarIn: JarInputStream, jarOut: JarOutputStream) -> Unit) =
        first.inputStream().nestedUse(::JarInputStream) { jarIn ->
            second.outputStream().nestedUse(::JarOutputStream) { jarOut ->
                block(jarIn, jarOut)
            }
        }

    private inline fun <T: Closeable, U: Closeable> T.nestedUse(nest: (T) -> U, block: (U) -> Unit) =
        use { nest(it).use(block) }

    companion object {
        fun Project.registerStripKotlinMetadataAttribute(name: String, configure: Parameters.() -> Unit = {}): Attribute<Boolean> {
            val attribute = Attribute.of(name, Boolean::class.javaObjectType)

            dependencies.registerTransform(StripKotlinMetadataTransform::class.java) {
                from.attribute(attribute, false)
                to.attribute(attribute, true)
                parameters(configure)
            }

            dependencies.artifactTypes.all {
                attributes.attribute(attribute, false)
            }

            return attribute
        }
    }
}
