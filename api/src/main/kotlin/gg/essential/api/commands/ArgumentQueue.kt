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
package gg.essential.api.commands

interface ArgumentQueue {
    /**
     * Poll the argument queue, getting the next string in the queue, and removing it from the queue.
     * This method will throw an exception if there are no arguments left.
     */
    fun poll(): String

    /**
     * Peek into the argument queue without removing it. If there are no arguments left, the result will be null.
     */
    fun peek(): String?

    /**
     * Whether any more arguments have been passed. This is equivalent to [peek] returning null.
     *
     * @return true if no arguments are left
     */
    fun isEmpty(): Boolean
}
