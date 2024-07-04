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
// Based on https://github.com/KevinPriv/keventbus/blob/c52e0a2ea0922c2fb94b99a707f1ae2ddf48a242/src/main/java/me/kbrewster/eventbus/eventbus.kt
//
//     MIT License
//
//     Copyright (c) 2020 Kevin Brewster
//
//     Permission is hereby granted, free of charge, to any person obtaining a copy
//     of this software and associated documentation files (the "Software"), to deal
//     in the Software without restriction, including without limitation the rights
//     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//     copies of the Software, and to permit persons to whom the Software is
//     furnished to do so, subject to the following conditions:
//
//     The above copyright notice and this permission notice shall be included in all
//     copies or substantial portions of the Software.
//
//     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//     SOFTWARE.

package gg.essential.util

import me.kbrewster.eventbus.EventBus.Subscriber
import me.kbrewster.eventbus.Subscribe
import me.kbrewster.eventbus.collection.ConcurrentSubscriberArrayList
import me.kbrewster.eventbus.collection.SubscriberArrayList
import me.kbrewster.eventbus.exception.ExceptionHandler
import me.kbrewster.eventbus.invokers.InvokerType
import me.kbrewster.eventbus.invokers.ReflectionInvoker
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EventBus @JvmOverloads constructor(
    private val invokerType: InvokerType = ReflectionInvoker(),
    private val exceptionHandler: ExceptionHandler = object: ExceptionHandler {
        override fun handle(exception: Exception) {
            throw exception
        }
    },
    private val threadSaftey: Boolean = true) {

    private val subscribers: AbstractMap<Class<*>, MutableList<Subscriber>> =
        if(threadSaftey) ConcurrentHashMap() else HashMap()

    /**
     * Subscribes all of the methods marked with the `@Subscribe` annotation
     * within the `obj` instance provided to th methods first parameter class
     *
     * e.g. registering an instance which includes the method below will invoke
     * that method every time EventBus#post(MessageReceivedEvent()) is called.
     * @Subscribe
     * fun messageReceivedEvent(event: MessageReceivedEvent) {
     * }
     *
     */
    fun register(obj: Any) {
        for (method in obj.javaClass.declaredMethods) {
            val sub: Subscribe = method.getAnnotation(Subscribe::class.java) ?: continue

            // verification
            val parameterClazz = method.parameterTypes[0]
            when {
                method.parameterCount != 1 -> throw IllegalArgumentException("Subscribed method must only have one parameter.")
                method.returnType != Void.TYPE -> throw IllegalArgumentException("Subscribed method must be of type 'Void'. ")
                parameterClazz.isPrimitive -> throw IllegalArgumentException("Cannot subscribe method to a primitive.")
                parameterClazz.modifiers and (Modifier.ABSTRACT or Modifier.INTERFACE) != 0 -> throw IllegalArgumentException("Cannot subscribe method to a polymorphic class.")
            }

            val subscriberMethod = invokerType.setup(obj, obj.javaClass, parameterClazz, method)

            val subscriber = Subscriber(obj, sub.priority, subscriberMethod)
            subscribers.putIfAbsent(parameterClazz, if(threadSaftey) ConcurrentSubscriberArrayList() else SubscriberArrayList())
            subscribers[parameterClazz]!!.add(subscriber)
        }
    }

    inline fun <reified T> register(noinline listener: (T) -> Unit, priority: Int = 0) = register(T::class.java, listener, priority)

    fun <T> register(cls: Class<T>, listener: (T) -> Unit, priority: Int = 0) {
        val subscriber = Subscriber(listener, priority) {
            @Suppress("UNCHECKED_CAST")
            listener(it as T)
        }
        subscribers.putIfAbsent(cls, if(threadSaftey) ConcurrentSubscriberArrayList() else SubscriberArrayList())
        subscribers[cls]!!.add(subscriber)
    }

    /**
     * Unsubscribes all `@Subscribe`'d methods inside of the `obj` instance.
     */
    fun unregister(obj: Any) {
        for (method in obj.javaClass.declaredMethods) {
            if (method.getAnnotation(Subscribe::class.java) == null) {
                continue
            }
            subscribers[method.parameterTypes[0]]?.remove(Subscriber(obj, -1, null))
        }
    }

    inline fun <reified T> unregister(noinline listener: (T) -> Unit) = unregister(T::class.java, listener)

    fun <T> unregister(cls: Class<T>, listener: (T) -> Unit) {
        subscribers[cls]?.remove(Subscriber(listener, -1, null))
    }

    /**
     * Posts the event instance given to all the subscribers
     * that are subscribed to the events class.
     */
    fun post(event: Any) {
        val events = subscribers[event.javaClass] ?: return
        // executed in descending order
        for (i in (events.size-1) downTo 0) {
            try {
                events[i].invoke(event)
            } catch (e: Exception) {
                exceptionHandler.handle(e)
            }
        }
    }

    /**
     * Supplier is only used if there are subscribers listening to
     * the event.
     *
     * Example usage: EventBus#post { ComputationallyHeavyEvent() }
     *
     * This allows events to only be constructed if needed.
     */
    inline fun <reified T> post(supplier: () -> T) {
        val events = getSubscribedEvents(T::class.java) ?: return
        val event = supplier()
        // executed in descending order
        for (i in (events.size-1) downTo 0) {
            events[i].invoke(event)
        }
    }

    fun getSubscribedEvents(clazz: Class<*>) = subscribers[clazz]

}
