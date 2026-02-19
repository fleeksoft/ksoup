package com.fleeksoft.ksoup.internal

internal actual class WeakReference<T : Any> actual constructor(referred: T) {
    val weekRefValue = java.lang.ref.WeakReference(referred)
    actual fun get(): T? = weekRefValue.get()
}