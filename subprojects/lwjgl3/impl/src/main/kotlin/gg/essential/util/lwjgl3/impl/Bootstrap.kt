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
package gg.essential.util.lwjgl3.impl

import gg.essential.config.AccessedViaReflection
import org.lwjgl.system.Configuration
import java.nio.file.Path

@AccessedViaReflection("Lwjgl3Loader")
@Suppress("unused")
object Bootstrap {

    /**
     * Set to true when the current GL context supports GL3
     * and false when it is based on GL2
     */
    lateinit var gl3: Lazy<Boolean>

    @AccessedViaReflection("Lwjgl3Loader")
    @JvmStatic
    fun init(nativesDir: Path, gl3: Lazy<Boolean>) {
        this.gl3 = gl3

        // We need LWJGL to extract its natives into a dedicated directory because any one native file can only be
        // loaded by one class loader.
        // By choosing a dedicated directory, we effectively also make dedicated native files.
        Configuration.SHARED_LIBRARY_EXTRACT_DIRECTORY.set(nativesDir.toAbsolutePath().toString())
        // PATH takes priority and is set as of Minecraft 1.20, but unlike DIRECTORY it doesn't create a separate folder
        // for each lwjgl version, which makes sense in vanilla because there is already a separate folder per MC
        // instance. But for Essential where multiple instances may share the same essential and therefore natives
        // folder, we still want to use a separate folder per lwjgl version, so we'll simply un-set this and have lwjgl
        // fall back to DIRECTORY as before.
        Configuration.SHARED_LIBRARY_EXTRACT_PATH.set(null)
        // We don't ship the required classes or natives for the jemalloc allocator (which the default), so LWJGL will
        // always fallback to the system allocator. However, if these classes and natives are available on the classpath
        // or java.library.path (as they are in modern Minecraft versions), LWJGL will attempt to use the jemalloc
        // allocator. If the LWJGL version we ship and the LWJGL version of the discovered jemalloc module are
        // mismatched, we could end up with a broken memory allocator. Forcing the memory allocator to system here
        // avoids this.
        Configuration.MEMORY_ALLOCATOR.set("system")
    }
}