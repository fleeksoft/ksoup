package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.stream.StreamCharReader

interface KsoupEngine {
    fun urlResolveOrNull(base: String, relUrl: String): String?

    fun openBufferReader(content: String, charset: Charset? = null): BufferReader

    fun openBufferReader(byteArray: ByteArray): BufferReader

    fun toStreamCharReader(
        bufferReader: BufferReader,
        charset: Charset = Charsets.UTF8,
        chunkSize: Int = SharedConstants.DefaultBufferSize
    ): StreamCharReader

    fun toStreamCharReader(
        content: String,
        charset: Charset = Charsets.UTF8,
        chunkSize: Int = SharedConstants.DefaultBufferSize
    ): StreamCharReader

    fun toStreamCharReader(
        byteArray: ByteArray,
        charset: Charset = Charsets.UTF8,
        chunkSize: Int = SharedConstants.DefaultBufferSize
    ): StreamCharReader

    fun getUtf8Charset(): Charset

    fun charsetForName(name: String): Charset
}