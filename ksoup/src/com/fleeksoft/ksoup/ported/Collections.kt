package com.fleeksoft.ksoup.ported

internal class Collections {
    companion object {
        fun <T> unmodifiableList(list: List<T>): List<T> {
            return list.toList()
        }
    }
}
