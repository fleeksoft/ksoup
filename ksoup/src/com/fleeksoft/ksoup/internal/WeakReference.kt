package com.fleeksoft.ksoup.internal

internal expect class WeakReference<T: Any>(referred: T) {
    fun get(): T?
}