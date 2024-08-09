package com.fleeksoft.ksoup.ported

import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value

class ThreadLocal<T>(val defaultValue: () -> T) {
    private val threadLocalRef = ThreadLocalRef<T>()


    fun get(): T {
        return threadLocalRef.get() ?: defaultValue().also { threadLocalRef.value = it }
    }

    fun setValue(value: T) {
        threadLocalRef.value = value
    }

}