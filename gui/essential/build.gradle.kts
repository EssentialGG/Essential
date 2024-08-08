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
import essential.universalLibs
import gg.essential.gradle.util.KotlinVersion

plugins {
    kotlin("jvm")
    id("gg.essential.defaults")
}

universalLibs()

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinVersion.minimal.stdlib))
    implementation(project(":feature-flags"))
    implementation(project(":utils"))
    implementation(project(":libs"))
    implementation(project(":infra"))
    implementation(project(":cosmetics", configuration = "minecraftRuntimeElements"))
    implementation(project(":gui:elementa"))
    implementation(project(":ice"))
    implementation(project(":quic-connector"))

    implementation("org.jitsi:ice4j:3.0-52-ga9ba80e") {
        isTransitive = false // for PseudoTCPBase only
    }
}

kotlin.jvmToolchain(8)

tasks.compileKotlin {
    kotlinOptions {
        moduleName = "essential" + project.path.replace(':', '-').lowercase()
    }
}
