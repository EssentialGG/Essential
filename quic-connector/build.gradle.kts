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
import gg.essential.gradle.util.prebundle

plugins {
    `java-library`
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
}

val netty by configurations.creating

configurations.compileOnly.configure {
    extendsFrom(netty)
}

dependencies {
    for (classifier in listOf("linux-x86_64", "osx-x86_64", "osx-aarch_64", "windows-x86_64")) {
        netty("io.netty.incubator:netty-incubator-codec-native-quic:0.0.50.Final:$classifier")
    }
    netty("org.bouncycastle:bcpkix-jdk15on:1.69") // for generating a self-signed cert

    api(prebundle(netty, jijName = "gg/essential/sps/quic/jvm/netty.jar") {
        exclude("META-INF/INDEX.LIST") // list of packages, invalid for fat jar
        exclude("META-INF/*.SF", "META-INF/*.DSA") // signatures, broken for fat jar
    })

    compileOnly(project(":feature-flags"))
}
