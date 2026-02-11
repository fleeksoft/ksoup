@file:OptIn(ExperimentalNativeApi::class)

package com.fleeksoft.ksoup.internal

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference as NativeWeakReference

internal actual class WeakReference<T : Any> actual constructor(referred: T) {
    private val weekRefValue = NativeWeakReference(referred)
    actual fun get(): T? = weekRefValue.get()
}