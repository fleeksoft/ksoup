package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.io.openBufferReader
import com.fleeksoft.ksoup.ported.stream.StreamCharReader
import com.fleeksoft.ksoup.ported.stream.StreamCharReaderImpl
import de.cketti.codepoints.appendCodePoint
import korlibs.memory.ByteArrayBuilder

internal fun String.isCharsetSupported(): Boolean {
    val result = runCatching { Charset.forName(this) }.getOrNull()
    return result != null
}

fun String.toByteArray(charset: Charset, start: Int = 0, end: Int = this.length): ByteArray {
    val out = ByteArrayBuilder(charset.estimateNumberOfBytesForCharacters(end - start))
    charset.encode(out, this, start, end)
    return out.toByteArray()
}

fun ByteArray.toString(charset: Charset = Charsets.UTF8, start: Int = 0, end: Int = this.size): String {
    val out = StringBuilder(charset.estimateNumberOfCharactersForBytes(end - start))
    charset.decode(out, this, start, end)
    return out.toString()
}

public fun BufferReader.toStreamCharReader(
    charset: Charset = Charsets.UTF8,
    chunkSize: Int = SharedConstants.DefaultBufferSize,
): StreamCharReader = StreamCharReaderImpl(bufferReader = this, charset = charset, chunkSize = chunkSize)

public fun String.toStreamCharReader(
    charset: Charset = Charsets.UTF8,
    chunkSize: Int = SharedConstants.DefaultBufferSize,
): StreamCharReader {
    return StreamCharReaderImpl(bufferReader = this.openBufferReader(charset = charset), charset = charset, chunkSize = chunkSize)
}

public fun ByteArray.toStreamCharReader(
    charset: Charset = Charsets.UTF8,
    chunkSize: Int = SharedConstants.DefaultBufferSize,
): StreamCharReader {
    return StreamCharReaderImpl(bufferReader = this.openBufferReader(), charset = charset, chunkSize = chunkSize)
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
