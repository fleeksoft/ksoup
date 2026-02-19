package com.fleeksoft.ksoup.internal

internal actual class WeakReference<T : Any> actual constructor(val referred: T) {
    actual fun get(): T? = referred
}