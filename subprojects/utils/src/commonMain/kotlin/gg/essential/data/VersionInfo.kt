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
package gg.essential.data

open class VersionInfo {
    companion object {
        const val noSavedVersion = "0.0.0"
    }

    val essentialVersion: String = System.getProperty("essential.version", noSavedVersion)
    val essentialBranch: String = System.getProperty("essential.branch", "stable")
    val essentialCommit: String by lazy {
        val commitFile = this::class.java.getResource("/assets/essential/commit.txt")
        if (commitFile != null) {
            return@lazy commitFile.readText().trim()
        }

        val version = this::class.java.getResource("/assets/essential/version.txt")!!.readText()
        val hash = version.split("+").last()

        if (hash.startsWith("g")) {
            hash.drop(1)
        } else if (version == "\${version}"){
            "dev"
        } else {
            "SNAPSHOT"
        }
    }
}
