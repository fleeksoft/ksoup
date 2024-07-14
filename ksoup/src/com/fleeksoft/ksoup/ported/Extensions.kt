package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.internal.SharedConstants
import de.cketti.codepoints.appendCodePoint
import korlibs.io.lang.Charset
import korlibs.io.lang.Charsets
import korlibs.io.stream.*
import korlibs.memory.ByteArrayBuilder

internal fun String.isCharsetSupported(): Boolean {
    val result = runCatching { Charset.forName(this) }.getOrNull()
    return result != null
}

public fun SyncStream.toStreamCharReader(
    charset: Charset = Charsets.UTF8,
    chunkSize: Int = SharedConstants.DefaultBufferSize,
): StreamCharReader = StreamCharReaderImpl(stream = this, charset = charset, chunkSize = chunkSize)

private fun handleQueryParams(
    relativePath: String,
    separator: String,
): MutableList<String> {
    val querySplit = relativePath.split(separator).toMutableList()
    val firstQueryPath = querySplit.removeFirst()
    val relativePathParts = firstQueryPath.split("/").toMutableList()
    if (querySplit.isNotEmpty()) {
        relativePathParts.add(
            "${relativePathParts.removeLastOrNull() ?: ""}$separator${querySplit.joinToString(separator)}",
        )
    }
    return relativePathParts
}

// TODO: handle it better

// some charsets can read but not encode; switch to an encodable charset and update the meta el
internal fun Charset.canEncode(): Boolean = runCatching { true }.getOrNull() != null

internal fun Charset.canEncode(c: Char): Boolean {
    return this.canEncode("$c")
}

internal fun Charset.canEncode(s: String): Boolean {
//    return true
    // TODO: check this
    return kotlin.runCatching { this.encode(ByteArrayBuilder(s.length * 8), s) }
        .onFailure {
            println("encodingErrro: $this")
            it.printStackTrace()
        }.isSuccess
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
