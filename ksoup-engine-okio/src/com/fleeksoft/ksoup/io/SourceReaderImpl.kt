package com.fleeksoft.ksoup.io

import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.buffer

internal class SourceReaderImpl : SourceReader {
    private val source: BufferedSource
    private var sourceMark: BufferedSource? = null

    constructor(bytes: ByteArray) {
        this.source = Buffer().apply { write(bytes) }
    }

    constructor(source: Source) {
        this.source = source.buffer()
    }

    private fun source(): BufferedSource = sourceMark ?: source

    override fun mark(readLimit: Long) {
        sourceMark = source().peek()
    }

    override fun reset() {
        sourceMark?.close()
        sourceMark = null
    }

    override fun readBytes(count: Int): ByteArray {
        val byteArray = ByteArray(count)
        var i = 0
        while (source().exhausted().not() && i < count) {
            byteArray[i] = source().readByte()
            i++
        }
        return if (i == 0) {
            byteArrayOf()
        } else if (i != count) {
            byteArray.sliceArray(0 until i)
        } else {
            byteArray
        }
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        return source().read(bytes, offset, byteCount = length)
    }

    override fun readAllBytes(): ByteArray {
        return source().readByteArray()
    }

    override fun exhausted(): Boolean {
        return source().exhausted()
    }

    override fun close() {
        return source().close()
    }

    override fun readAtMostTo(sink: KByteBuffer, byteCount: Int): Int {
        val bytes = readBytes(byteCount)
        sink.writeBytes(bytes, bytes.size)
        return bytes.size
    }
}