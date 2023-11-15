package com.fleeksoft.ksoup.ported

fun assert(condition: Boolean, error: String? = null) {
    if (!condition) {
        throw Exception(error ?: "Assert error!")
    }
}
