package com.fleeksoft.ksoup.ported

internal fun assert(
    condition: Boolean,
    error: String? = null,
) {
    if (!condition) {
        throw Exception(error ?: "Assert error!")
    }
}
