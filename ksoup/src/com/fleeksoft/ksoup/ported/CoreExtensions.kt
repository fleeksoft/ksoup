package com.fleeksoft.ksoup.ported

import com.fleeksoft.charset.Charsets

internal fun String.isCharsetSupported(): Boolean {
    return Charsets.isSupported(this)
}

internal fun IntArray.codePointsToString(): String {
    return if (this.isNotEmpty()) {
        buildString {
            this@codePointsToString.forEach {
                appendCodePoint(it)
            }
        }
    } else {
        ""
    }
}

internal fun <E> ArrayList<E>.removeRange(fromIndex: Int, toIndex: Int) {
    for (i in (toIndex - 1) downTo fromIndex) {
        this.removeAt(i)
    }
}
