package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.Charsets
import de.cketti.codepoints.appendCodePoint

internal fun String.isCharsetSupported(): Boolean {
    val result = runCatching { Charsets.forName(this) }.getOrNull()
    return result != null
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

internal fun <E> ArrayList<E>.removeRange(
    fromIndex: Int,
    toIndex: Int,
) {
    for (i in (toIndex - 1) downTo fromIndex) {
        this.removeAt(i)
    }
}
