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
package essential

import gg.essential.gradle.multiversion.Platform
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.nameWithoutExtension

plugins {
    java
}

val platform: Platform by extensions

val dependency = when {
    platform.isFabric -> "gg.essential:loader-fabric:1.2.1"
    platform.isModLauncher && platform.mcVersion >= 11700 -> "gg.essential:loader-modlauncher9:1.2.1"
    platform.isModLauncher -> "gg.essential:loader-modlauncher8:1.2.1"
    platform.isLegacyForge -> "gg.essential:loader-launchwrapper:1.2.1"
    else -> throw UnsupportedOperationException("No known loader variant for current platform.")
}

val loader: Configuration by configurations.creating

dependencies {
    loader(dependency)
}

tasks.jar {
    if (platform.isLegacyForge) {
        dependsOn(loader)
        from({ loader.map { zipTree(it) } })

        manifest {
            attributes(
                "FMLModType" to "LIBRARY",
                "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
                "TweakOrder" to "0",
            )
        }
    }

    if (platform.isFabric) {
        // We include the jar ourselves (and configure our fabric.mod.json accordingly), so we have control over where
        // the embedded jars are located. This is important because the default location of `META-INF/jars` gets special
        // treatment by loader-stage2, and we don't want that for the embedded loader.
        from(loader) {
            rename { "essential-loader.jar" }
        }
    }
}

/**
 * Takes the stage1 jar from the input stage0 jar and copies it to a different location in the output jar.
 * We do this so we don't need to do any double-unpacking at runtime when we upgrade the installed stage1 version, and
 * because the raw loader stored above will actually be stripped when the Essential jar is installed via stage2 (because
 * the regular upgrade path would break with relaunching; we instead need to use the `stage1.update.jar` path).
 */
abstract class Stage1JarTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile.toPath()
        val output = outputs.file(input.nameWithoutExtension + "-stage1-only.jar").toPath()
        FileSystems.newFileSystem(input, null as ClassLoader?).use { sourceFs ->
            val source = sourceFs.getPath("gg/essential/loader/stage0/stage1.jar")
            FileSystems.newFileSystem(output, mapOf("create" to true)).use { targetFs ->
                val target = targetFs.getPath("gg/essential/loader-stage1.jar")
                Files.createDirectories(target.parent)
                Files.copy(source, target)
            }
        }
    }
}

dependencies {
    val attr = Attribute.of("stage1Jar", Boolean::class.javaObjectType)
    dependencies.registerTransform(Stage1JarTransform::class.java) {
        from.attribute(attr, false)
        to.attribute(attr, true)
    }
    dependencies.artifactTypes.all {
        attributes.attribute(attr, false)
    }
    // Note: May need to pre-bundle this if we ever want to have the real thing on the classpath too; each dependency
    //       can only be present once per configuration.
    implementation("bundle"(dependency) {
        attributes { attribute(attr, true) }
    })
}
