package com.fleeksoft.ksoup.helper

fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, valueFunc: () -> V): V {
    if (!this.containsKey(key)) {
        this[key] = valueFunc()
    }

    return this[key]!!
}