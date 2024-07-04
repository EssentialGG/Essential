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

import com.google.common.hash.Hashing
import gg.essential.gradle.multiversion.Platform
import gg.essential.gradle.util.CONSTANT_TIME_FOR_ZIP_ENTRIES
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    java
}

val platform: Platform by extensions

val loaderVariant = when {
    platform.isFabric -> "fabric"
    platform.isModLauncher && platform.mcVersion >= 11700 -> "modlauncher9"
    platform.isModLauncher -> "modlauncher8"
    platform.isLegacyForge -> "launchwrapper"
    else -> throw UnsupportedOperationException("No known loader variant for current platform.")
}

val loaderContainer: Configuration by configurations.creating
val loaderStage2: Configuration by configurations.creating

dependencies {
    loaderContainer("gg.essential.loader:container-$loaderVariant:included") { isTransitive = false }
    loaderStage2("gg.essential.loader:stage2-$loaderVariant:included") { isTransitive = false }
}

tasks.named<Jar>("bundleJar") {
    val loaderStage2Properties = layout.buildDirectory.file("essential-loader-stage2.properties")

    dependsOn(loaderContainer)
    from({ zipTree(loaderContainer.singleFile) }) {
        into("pinned")
    }
    from(loaderStage2) {
        into("pinned")
    }
    from(loaderStage2Properties) {
        into("pinned")
    }

    val loaderStage2Version = provider {
        loaderStage2.resolvedConfiguration.resolvedArtifacts.single().moduleVersion.id.version.also {
            assert(it != "included") // we expect the actual version of the included build, not our dummy version
        }
    }
    inputs.property("stage2Version", loaderStage2Version)
    doFirst {
        // Not using java.util.Properties because we want reproducible results (fairly sure the values won't need
        // escaping anyway, so this should be decently safe)
        loaderStage2Properties.get().asFile.writeText("""
            pinnedFile=/${loaderStage2.singleFile.name}
            pinnedFileVersion=${loaderStage2Version.get()}
            pinnedFileMd5=${Hashing.md5().hashBytes(loaderStage2.singleFile.readBytes())}
        """.trimIndent())
    }
}

abstract class PinnedJar : DefaultTask() {
    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputJar: RegularFileProperty

    @get:Input
    abstract val inputVersion: Property<String>

    @TaskAction
    fun convertToPinnedJar() {
        val input = inputJar.get().asFile
        val output = outputJar.get().asFile

        fun constantZipEntry(name: String) = ZipEntry(name).apply { time = CONSTANT_TIME_FOR_ZIP_ENTRIES }

        output.outputStream().use { fileOut ->
            ZipOutputStream(fileOut).use { zipOut ->
                val inputBytes = input.readBytes()
                val inputMd5 = Hashing.md5().hashBytes(inputBytes).toString()

                ZipInputStream(inputBytes.inputStream()).use { zipIn ->
                    while (true) {
                        val entry = zipIn.nextEntry ?: break
                        if (!entry.name.startsWith("pinned/")) continue
                        val targetName = entry.name.removePrefix("pinned/")
                        if (targetName.isEmpty()) continue
                        zipOut.putNextEntry(constantZipEntry(targetName))
                        zipIn.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }

                zipOut.putNextEntry(constantZipEntry("essential-$inputMd5.jar"))
                zipOut.write(inputBytes)
                zipOut.putNextEntry(constantZipEntry("essential-loader.properties"))
                zipOut.write("""
                    pinnedFile=/essential-$inputMd5.jar
                    pinnedFileVersion=${inputVersion.get()}
                    pinnedFileMd5=$inputMd5
                    publisherSlug=essential
                    modSlug=essential
                    displayName=Essential
                """.trimIndent().encodeToByteArray())
            }
        }
    }
}

val pinnedJar by tasks.registering(PinnedJar::class) {
    outputJar.set(layout.buildDirectory.file(provider {
        val modVersion = project.version.toString().replace('.', '-')
        val mcVersion = platform.mcVersionStr.replace('.', '-')
        "libs/pinned_essential_${modVersion}_${platform.loaderStr}_${mcVersion}.jar"
    }))
    inputJar.set(tasks.named("relocatedJar", Jar::class).flatMap { it.archiveFile })
    inputVersion.set(provider { project.version.toString() })
}
tasks.assemble { dependsOn(pinnedJar) }
