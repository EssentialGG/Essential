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
import gg.essential.gradle.util.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("java-library")
    id("gg.essential.defaults")
}

kotlin {
    jvm("minecraft")

    sourceSets["commonMain"].dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.30")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.2")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
        implementation(project(":feature-flags"))
        api(project(":utils"))
    }

    sourceSets["commonTest"].dependencies {
        implementation(kotlin("test"))
    }

    sourceSets["minecraftMain"].dependencies {
        compileOnly("gg.essential:universalcraft-1.8.9-forge:228") { isTransitive = false }
    }
}

kotlin.jvmToolchain(8)
tasks.withType(KotlinCompile::class) { setJvmDefault("all") }
