package com.fleeksoft.ksoup.helper

fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, valueFunc: (K) -> V): V {
    if (!this.containsKey(key)) {
        this[key] = valueFunc(key)
    }

    return this[key]!!
}