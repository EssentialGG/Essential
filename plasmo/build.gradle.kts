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
import essential.modrinth

plugins {
    `java-library`
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    modrinth()
}

dependencies {
    implementation(project(":feature-flags"))
    compileOnly("javax.inject:javax.inject:1")
    compileOnly("maven.modrinth:plasmo-voice:fabric-1.20-2.0.5")
}
