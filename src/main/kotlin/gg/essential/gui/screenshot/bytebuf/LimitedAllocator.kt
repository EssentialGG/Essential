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
package gg.essential.gui.screenshot.bytebuf

import io.netty.buffer.AbstractByteBufAllocator
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

class LimitedAllocator(
    private val alloc: ByteBufAllocator,
    private val limit: Long,
) : AbstractByteBufAllocator() {
    private val allocatedBytes = AtomicLong()

    fun getAllocatedBytes() = allocatedBytes.get()

    fun tryHeapBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf? =
        tryAlloc(initialCapacity, maxCapacity, ::heapBuffer)

    fun tryDirectBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf? =
        tryAlloc(initialCapacity, maxCapacity, ::directBuffer)

    private fun tryAlloc(
        initialCapacity: Int,
        maxCapacity: Int,
        alloc: (initialCapacity: Int, maxCapacity: Int) -> ByteBuf,
    ): ByteBuf? {
        if (initialCapacity > limit) {
            // This allocation is eternally doomed, so instead of live-locking it, let's just grant it
            return alloc(initialCapacity, maxCapacity)
        }

        // First try to reserve space for us
        var prev: Long
        var next: Long
        do {
            prev = allocatedBytes.get()
            next = prev + initialCapacity
            if (next > limit) {
                return null
            }
        } while (!allocatedBytes.compareAndSet(prev, next))

        // then allocate the buffer
        return alloc(initialCapacity, maxCapacity).also {
            // finally free the reservation again (the buffer itself now holds the memory)
            allocatedBytes.addAndGet(-initialCapacity.toLong())
        }
    }

    private fun trackNew(buf: ByteBuf): TrackedByteBuf {
        val trackedCapacity = buf.capacity()
        allocatedBytes.addAndGet(trackedCapacity.toLong())
        return TrackedByteBuf(buf, trackedCapacity)
    }

    override fun newHeapBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf =
        trackNew(alloc.heapBuffer(initialCapacity, maxCapacity))

    override fun newDirectBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf =
        trackNew(alloc.directBuffer(initialCapacity, maxCapacity))

    override fun isDirectBufferPooled(): Boolean = alloc.isDirectBufferPooled

    private inner class TrackedByteBuf(buf: ByteBuf, private val trackedCapacity: Int) : WrappedByteBuf(buf) {

        private fun trackShared(buf: ByteBuf): TrackedByteBuf {
            return buf as? TrackedByteBuf ?: TrackedByteBuf(buf, trackedCapacity)
        }

        private fun deallocate() {
            allocatedBytes.addAndGet(-trackedCapacity.toLong())
        }

        override fun release(): Boolean {
            if (super.release()) {
                deallocate()
                return true
            }
            return false
        }

        override fun release(decrement: Int): Boolean {
            if (super.release(decrement)) {
                deallocate()
                return true
            }
            return false
        }

        override fun alloc(): ByteBufAllocator {
            return this@LimitedAllocator
        }

        override fun slice(): ByteBuf {
            return trackShared(super.slice())
        }

        //#if MC>=11200
        override fun retainedSlice(): ByteBuf {
            return trackShared(super.retainedSlice())
        }

        override fun retainedSlice(index: Int, length: Int): ByteBuf {
            return trackShared(super.retainedSlice(index, length))
        }

        override fun retainedDuplicate(): ByteBuf {
            return trackShared(super.retainedDuplicate())
        }

        override fun readRetainedSlice(length: Int): ByteBuf {
            return trackShared(super.readRetainedSlice(length))
        }
        //#endif

        override fun slice(index: Int, length: Int): ByteBuf {
            return trackShared(super.slice(index, length))
        }

        override fun duplicate(): ByteBuf {
            return trackShared(super.duplicate())
        }

        override fun readSlice(length: Int): ByteBuf {
            return trackShared(super.readSlice(length))
        }

        //#if MC>=11200
        override fun asReadOnly(): ByteBuf {
            return trackShared(super.asReadOnly())
        }
        //#endif

        override fun order(endianness: ByteOrder): ByteBuf {
            return trackShared(super.order(endianness))
        }

        override fun readBytes(length: Int): ByteBuf {
            return trackNew(super.readBytes(length))
        }

        override fun copy(): ByteBuf {
            return trackNew(super.copy())
        }

        override fun copy(index: Int, length: Int): ByteBuf {
            return trackNew(super.copy(index, length))
        }
    }
}