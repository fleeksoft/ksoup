package com.fleeksoft.ksoup.ported

class AtomicBoolean(value: Boolean) {
    private var value: Boolean = value
    fun set(value: Boolean) {
        this.value = value
    }

    fun get() = value
}
