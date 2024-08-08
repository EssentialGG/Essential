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
plugins {
    id("java-library")
    id("gg.essential.defaults")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

dependencies {
    api(libs.slf4j.api)

    compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
    compileOnly(project(":feature-flags"))
}
