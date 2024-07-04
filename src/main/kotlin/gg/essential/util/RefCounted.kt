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
package gg.essential.util

/**
 * An on-demand allocated, reference-counted box for a T.
 *
 * Each call to [obtain] must be logically paired with one and only one call to [release].
 */
class RefCounted<T : Any> {
    private var refCount: Int = 0
    private var value: T? = null

    /**
     * Increments the reference count and returns the value. The [factory] is invoked if no value exists yet.
     */
    @Synchronized
    fun obtain(factory: () -> T): T {
        refCount++
        return value ?: factory().also { value = it }
    }

    /**
     * Decrements the reference count. If the count reaches zero, the value is removed and handed to the [disposer].
     */
    @Synchronized
    fun release(disposer: (value: T) -> Unit) {
        refCount--
        if (refCount == 0) {
            disposer(value!!)
            value = null
        }
    }

}