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

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class RelaxFabricLoaderDependencyTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val output = outputs.file(input.nameWithoutExtension + "-relaxed-fabricloader.jar")

        output.outputStream().use { fileOut ->
            ZipOutputStream(fileOut).use { out ->
                ZipFile(input).use { zipFile ->
                    for (entry in zipFile.entries()) {
                        out.putNextEntry(ZipEntry(entry.name).apply { time = CONSTANT_TIME_FOR_ZIP_ENTRIES })
                        if (entry.name == "fabric.mod.json") {
                            val json = zipFile.getInputStream(entry).readAllBytes().decodeToString()
                            val relaxedJson = json.replace(Regex(""""fabricloader" *: *"[^"]+""""), """"fabricloader": "*"""")
                            out.write(relaxedJson.encodeToByteArray())
                        } else {
                            zipFile.getInputStream(entry).copyTo(out)
                        }
                        out.closeEntry()
                    }
                }
            }
        }
    }
}
