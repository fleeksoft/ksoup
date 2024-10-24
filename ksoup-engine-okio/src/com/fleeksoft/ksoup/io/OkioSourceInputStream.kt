package com.fleeksoft.ksoup.io

import com.fleeksoft.io.InputStream
import okio.BufferedSource

class OkioSourceInputStream(private val source: BufferedSource) : InputStream() {
    private var sourceMark: BufferedSource? = null

    private fun source(): BufferedSource = sourceMark ?: source

    override fun mark(readLimit: Int) {
        sourceMark = source().peek()
    }

    override fun markSupported(): Boolean {
        return true
    }

    override fun reset() {
        sourceMark?.close()
        sourceMark = null
    }

    override fun read(): Int {
        if (source().exhausted()) {
            return -1
        }
        return source().readByte().toInt() and 0xff
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        return source().read(bytes, off, byteCount = len)
    }

    override fun readAllBytes(): ByteArray {
        return source().readByteArray()
    }

    override fun available(): Int {
        return minOf(source().buffer.size.toInt(), Int.MAX_VALUE - 1)
    }

    fun exhausted(): Boolean {
        return source().exhausted()
    }

    override fun close() {
        return source().close()
    }
}