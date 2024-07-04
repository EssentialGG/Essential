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
import essential.*
import gg.essential.gradle.util.*

plugins {
    id("kotlin")
    id("org.jetbrains.dokka")
    id("gg.essential.defaults")
    id("gg.essential.defaults.maven-publish")
    id("gg.essential.multi-version")
}

val mcVersionStr = platform.mcVersionStr
val mcPlatform = platform.loaderStr

group = "gg.essential"
base.archivesName.set("EssentialAPI " + project.name)
tasks.compileKotlin { kotlinOptions.moduleName = "essential-api" }
java.withSourcesJar()
loom.noRunConfigs() // can't run just the API, only the implementation
configureDokkaForEssentialApi()

// We need to use the compatibility mode on old versions because we used to use the old Kotlin defaults for those
tasks.compileKotlin.setJvmDefault(if (platform.mcVersion >= 11400) "all" else "all-compatibility")

repositories {
    mavenLocal()
}

dependencies {
    // Kotlin
    val kotlin = platform.kotlinVersion
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin.stdlib}")
    api("org.jetbrains.kotlin:kotlin-reflect:${kotlin.stdlib}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlin.coroutines}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${kotlin.coroutines}")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:${kotlin.serialization}")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlin.serialization}")
    compileOnly("org.jetbrains:annotations:23.0.0")

    // Core Libraries
    api("org.kodein.di:kodein-di-jvm:7.6.0")

    // Due to loom's remapping, Gradle may not always select the latest version and multiple fabric-loader versions can
    // end up on the dev classpath.
    configurations.modApi.configure { exclude(group = "net.fabricmc", module = "fabric-loader") }

    // Core Gui Libraries
    val ucMcVersion = when (platform.mcVersion) {
        11900, 11902, 11903, 11904, 12000, 12001, 12002, 12004, 12006, 12100 -> mcVersionStr.also {
            // Elementa and Vigilance 1.18.1 are good enough for MC 1.19 so we only update UC.
            // We do need to exclude the tranitive 1.18 UC though.
            configurations.modApi.configure { exclude("gg.essential", "universalcraft-1.18.1-${platform.loaderStr}") }
        }
        11802 -> "1.18.1"
        else -> mcVersionStr
    }
    val libMcVersion = if (platform.mcVersion >= 11802) "1.18.1" else mcVersionStr
    // These versions are configured in gradle/libs.versions.toml
    modApi("gg.essential:vigilance-${libMcVersion}-${mcPlatform}:${libs.versions.vigilance.get()}") { exclude(group = "org.jetbrains.kotlin") }
    modApi("gg.essential:universalcraft-${ucMcVersion}-${mcPlatform}:${libs.versions.universalcraft.get()}") { exclude(group = "org.jetbrains.kotlin") }
    modApi("gg.essential:elementa-${libMcVersion}-${mcPlatform}:${libs.versions.elementa.get()}") { exclude(group = "org.jetbrains.kotlin") }

    // Miscellaneous Utility Libraries
    api("com.github.videogame-hacker:Koffee:88ba1b0") {
        exclude(module = "asm-commons")
        exclude(module = "asm-tree")
        exclude(module = "asm")
    }
    api("gg.essential.lib:caffeine:2.9.0") // keep in sync with `/libs/build.gradle.kts`
    api("gg.essential.lib:mixinextras:${libs.versions.mixinextras.get()}")
}
