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
package gg.essential.util.lwjgl3

import gg.essential.util.classloader.RelaunchClassLoader
import gg.essential.util.lwjgl3.asm.GLBridgeTransformer
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Loads our bundled LWJGL3 in a (mostly) isolated class loader as to not conflict with the system LWJGL (2 or 3).
 *
 * The only classes shared between the normal class loader and the isolated one, are those in the
 * [gg.essential.util.lwjgl3.api] package. Consequently, those can only depend on other classes from the same package
 * and on classes from the Java standard library.
 *
 * For convenient access, the loader provides a factory for classes implementing interfaces in the api package. These
 * classes must be named based on the interface they're implementing with an additional `Impl` suffix, and must be
 * located in the `impl` rather than the `api` package.
 */
class Lwjgl3Loader(nativesDir: Path, gl3: Lazy<Boolean>) {
    private val loader = RelaunchClassLoader(arrayOf(findExtractedBundleJar()), javaClass.classLoader, GLBridgeTransformer()).apply {
        // Our API package is the only (non-standard) package that's excluded from the isolation
        addPackageExclusion("$PKG_API.")
        // Above was a lie, ModLauncher's findResource fails when the class is in another layer. Instead of trying to
        // find complex workarounds for that, we'll just exclude Kotlin (provided by KotlinForForge) as well.
        // As a bonus, this theoretically allows us to use Kotlin types in our API.
        addPackageExclusion("kotlin.")
        addPackageExclusion("kotlinx.")
        // We'll also want to use netty in our API for buffer management.
        addPackageExclusion("io.netty.")
        // Invoke the bootstrap code which sets up LWJGL's natives extraction code to look in a dedicated directory
        loadClass("$PKG_IMPL.Bootstrap")
            .getMethod("init", Path::class.java, Lazy::class.java)
            .invoke(null, nativesDir, gl3)
    }

    inline fun <reified T> get(): T = get(T::class.java)

    fun <T> get(cls: Class<T>): T {
        if (!cls.name.startsWith(PKG_API)) {
            throw IllegalArgumentException("Can only provide implementations for interfaces from $PKG_API.")
        }
        val implName = "$PKG_IMPL${cls.name.removePrefix(PKG_API)}Impl"
        val implCls = loader.loadClass(implName)
        val impl = implCls.getDeclaredConstructor().newInstance()
        return cls.cast(impl)
    }

    companion object {
        private const val PKG = "gg.essential.util.lwjgl3"
        private const val PKG_API = "$PKG.api"
        private const val PKG_IMPL = "$PKG.impl"

        private var extractedBundleJar: URL? = null

        private fun findExtractedBundleJar() = extractedBundleJar ?: run {
            val bundleJarResource = Lwjgl3Loader::class.java.getResource("bundle.jar")
                ?: throw IllegalStateException("Failed to find lwjgl3 bundle jar")
            val tmpFile = Files.createTempFile("essential-lwjgl3", ".jar")
            tmpFile.toFile().deleteOnExit()
            bundleJarResource.openStream().use {
                Files.copy(it, tmpFile, StandardCopyOption.REPLACE_EXISTING)
            }
            tmpFile.toUri().toURL().also { extractedBundleJar = it }
        }
    }
}