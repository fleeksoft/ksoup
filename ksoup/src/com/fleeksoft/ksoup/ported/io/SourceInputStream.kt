package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.charset.Charset
import com.fleeksoft.io.BufferedReader
import com.fleeksoft.io.InputStream
import com.fleeksoft.io.InputStreamReader
import com.fleeksoft.io.buffered
import com.fleeksoft.io.reader
import com.fleeksoft.ksoup.io.SourceReader

class SourceInputStream(private val source: SourceReader) : InputStream() {
    override fun read(): Int {
        if (source.exhausted()) return -1
        return source.readInt()
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        return source.read(bytes, off, len)
    }

    override fun readAllBytes(): ByteArray {
        return source.readAllBytes()
    }

    override fun mark(readLimit: Int) {
        source.mark(readLimit.toLong())
    }

    override fun reset() {
        source.reset()
    }

    override fun markSupported(): Boolean {
        return true
    }

    override fun close() {
        source.close()
    }
}

fun sourceInputStreamReader(source: SourceReader, charset: Charset = Charsets.UTF8): InputStreamReader =
    SourceInputStream(source = source).reader(charset)

fun sourceInputStreamBufferedReader(source: SourceReader, charset: Charset = Charsets.UTF8): BufferedReader =
    sourceInputStreamReader(source, charset).buffered()