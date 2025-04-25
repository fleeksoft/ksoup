package com.fleeksoft.ksoup.internal

import kotlin.jvm.JvmInline

@JvmInline
internal value class WeakReference<T> constructor(private val referred: T) {
    fun get(): T? {
        return referred
    }
}