package com.fleeksoft.ksoup.ported

public class AtomicBoolean(private var value: Boolean) {

    public fun set(value: Boolean) {
        this.value = value
    }

    public fun get(): Boolean = value
}
