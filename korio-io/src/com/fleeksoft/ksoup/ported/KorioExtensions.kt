package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.*
import com.fleeksoft.ksoup.ported.stream.StreamCharReader
import com.fleeksoft.ksoup.ported.stream.StreamCharReaderImpl

fun String.openBufferReader(charset: Charset? = null): BufferReader =
    BufferReaderImpl(charset?.toByteArray(this) ?: this.encodeToByteArray())

fun ByteArray.openBufferReader(): BufferReader = BufferReaderImpl(this)
fun BufferReader.toStreamCharReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): StreamCharReader =
    StreamCharReaderImpl(bufferReader = this, charset = charset, chunkSize = chunkSize)

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