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

sealed class Try<out A> {
    inline fun <B> map(mapper: (A) -> B) =
        when (this) {
            is Failure -> this
            is Success -> Try { mapper(this.value) }
        }

    inline fun <B> force(failureVal: B, successVal: (A) -> B) =
        when (this) {
            is Failure -> Success(failureVal)
            is Success -> map(successVal)
        }

    inline fun resolve(failure: () -> Unit) =
        when (this) {
            is Failure -> {
                failure()
                null
            }
            is Success -> this.value
        }

    companion object {
        @Suppress("TooGenericExceptionCaught")
        inline operator fun <A> invoke(body: () -> A?) =
            try {
                val res = body()

                if (res == null) Failure else Success(res)
            } catch (e: Exception) {
                Failure
            }
    }
}

class Success<out A>(val value: A) : Try<A>()
object Failure : Try<Nothing>()

fun Try<*>.isSuccess() = this is Success

val Try<*>.asSuccess get() = this as Success

fun <A> Try<A>.recover(failureVal: A) = when (this) {
    is Failure -> Success(failureVal)
    is Success -> this
}
