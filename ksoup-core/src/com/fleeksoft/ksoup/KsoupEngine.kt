package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.KBuffer
import com.fleeksoft.ksoup.ported.io.Charset
import com.fleeksoft.ksoup.ported.io.SourceReader

interface KsoupEngine {
    fun urlResolveOrNull(base: String, relUrl: String): String?

    fun openSourceReader(content: String, charset: Charset? = null): SourceReader

    fun openSourceReader(byteArray: ByteArray): SourceReader

    fun getUtf8Charset(): Charset

    fun charsetForName(name: String): Charset

    fun newBufferInstance(): KBuffer
}