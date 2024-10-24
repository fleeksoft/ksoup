package com.fleeksoft.ksoup.io

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray

internal class SourceReaderImpl : SourceReader {
    private val source: Source
    private var sourceMark: Source? = null

    constructor(bytes: ByteArray) {
        this.source = Buffer().apply { write(bytes) }
    }

    constructor(source: Source) {
        this.source = source
    }

    fun source(): Source = sourceMark ?: source

    override fun mark(readLimit: Long) {
        sourceMark = source().peek()
    }

    override fun reset() {
        sourceMark?.close()
        sourceMark = null
    }

    override fun readInt(): Int {
        if (source.exhausted()) {
            return -1
        }
        return source.readByte().toInt() and 0xff
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
            byteArray.copyOfRange(0, i)
        } else {
            byteArray
        }
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        return source().readAtMostTo(bytes, offset, endIndex = offset + length)
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
}