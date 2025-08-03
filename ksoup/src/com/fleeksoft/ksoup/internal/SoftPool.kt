/*
 * Kotlin port of jsoup's SoftPool.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.internal

import com.fleeksoft.ksoup.ported.ThreadLocal


/**
 * A SoftPool is a ThreadLocal that holds a SoftReference to a pool of initializable objects. This allows us to reuse
 * expensive objects (buffers, etc.) between invocations (the ThreadLocal), but also for those objects to be reaped if
 * they are no longer in use.
 *
 * Like a ThreadLocal, should be stored in a static field.
 * @param <T> the type of object to pool.
</T> */
class SoftPool<T>(private val initializer: () -> T) {
    val threadLocalStack: ThreadLocal<ArrayDeque<T>> = ThreadLocal { ArrayDeque() }

    /**
     * Borrow an object from the pool, creating a new one if the pool is empty. Make sure to release it back to the pool
     * when done, so that it can be reused.
     * @return an object from the pool, as defined by the initializer.
     */
    fun borrow(): T {
        val stack: ArrayDeque<T> = stack
        if (!stack.isEmpty()) {
            return stack.removeAt(0)
        }
        return initializer()
    }

    /**
     * Release an object back to the pool. If the pool is full, the object is not retained. If you don't want to reuse a
     * borrowed object (for e.g. a StringBuilder that grew too large), just don't release it.
     * @param value the object to release back to the pool.
     */
    fun release(value: T) {
        val stack: ArrayDeque<T> = stack
        if (stack.size < MaxIdle) {
            stack.addFirst(value)
        }
    }

    val stack: ArrayDeque<T>
        get() = threadLocalStack.get()

    companion object {
        /**
         * How many total uses of the creating object might be instantiated on the same thread at once. More than this and
         * those objects aren't recycled. Doesn't need to be too conservative, as they can still be GCed as SoftRefs.
         */
        const val MaxIdle: Int = 12
    }
}
