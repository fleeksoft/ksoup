package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.BufferReaderImpl
import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.CharsetImpl
import com.fleeksoft.ksoup.ported.openBufferReader
import com.fleeksoft.ksoup.ported.stream.StreamCharReader
import com.fleeksoft.ksoup.ported.stream.StreamCharReaderImpl
import korlibs.io.lang.Charsets
import korlibs.io.net.URL

class KorioKsoupEngine : KsoupEngine {
    override fun urlResolveOrNull(base: String, relUrl: String): String? {
        return URL.resolveOrNull(base = base, access = relUrl)
    }

    override fun openBufferReader(
        content: String,
        charset: Charset?
    ): BufferReader {
        return BufferReaderImpl(charset?.toByteArray(content) ?: content.encodeToByteArray())
    }

    override fun openBufferReader(byteArray: ByteArray): BufferReader {
        return BufferReaderImpl(byteArray)
    }

    override fun toStreamCharReader(
        bufferReader: BufferReader,
        charset: Charset,
        chunkSize: Int
    ): StreamCharReader {
        return StreamCharReaderImpl(bufferReader = bufferReader, charset = charset, chunkSize = chunkSize)
    }

    override fun toStreamCharReader(
        content: String,
        charset: Charset,
        chunkSize: Int
    ): StreamCharReader {
        return StreamCharReaderImpl(bufferReader = content.openBufferReader(charset = charset), charset = charset, chunkSize = chunkSize)
    }

    override fun toStreamCharReader(
        byteArray: ByteArray,
        charset: Charset,
        chunkSize: Int
    ): StreamCharReader {
        return StreamCharReaderImpl(bufferReader = byteArray.openBufferReader(), charset = charset, chunkSize = chunkSize)
    }

    override fun getUtf8Charset(): Charset {
        return CharsetImpl(Charsets.UTF8)
    }

    override fun charsetForName(name: String): Charset {
        return CharsetImpl(name)
    }
}