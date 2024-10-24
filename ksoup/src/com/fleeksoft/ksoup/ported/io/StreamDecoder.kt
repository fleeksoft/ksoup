package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.charset.Charset
import com.fleeksoft.ksoup.io.SourceReader

expect class StreamDecoder(source: SourceReader, charset: Charset) : Reader {
    override fun read(cbuf: CharArray, offset: Int, length: Int): Int
    override fun ready(): Boolean
    override fun close()
    fun getEncoding(): String?
}