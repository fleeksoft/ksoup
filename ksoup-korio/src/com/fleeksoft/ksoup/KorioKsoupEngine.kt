package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.*
import korlibs.io.lang.Charsets
import korlibs.io.net.URL

class KorioKsoupEngine : KsoupEngine {
    override fun urlResolveOrNull(base: String, relUrl: String): String? {
        return URL.resolveOrNull(base = base, access = relUrl)
    }

    override fun openSourceReader(
        content: String,
        charset: Charset?
    ): SourceReader {
        return SourceReaderImpl(charset?.toByteArray(content) ?: content.encodeToByteArray())
    }

    override fun openSourceReader(byteArray: ByteArray): SourceReader {
        return SourceReaderImpl(byteArray)
    }

    override fun getUtf8Charset(): Charset {
        return CharsetImpl(Charsets.UTF8)
    }

    override fun charsetForName(name: String): Charset {
        return CharsetImpl(name)
    }

    override fun newBufferInstance(): Buffer = BufferImpl()
}