package com.fleeksoft.ksoup.io

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray

class SourceReaderImpl : SourceReader {
    private val source: Source
    private var sourceMark: Source? = null

    constructor(bytes: ByteArray) {
        this.source = Buffer().apply { write(bytes) }
    }

    constructor(source: Source) {
        this.source = source
    }

    fun source(): Source = sourceMark ?: source

    override fun skip(count: Long) {
        source().skip(count)
    }

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

    override fun read(): Byte {
        return source().readByte()
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

    override fun readAtMostTo(sink: KByteBuffer, byteCount: Int): Int {
        val bytes = readBytes(byteCount)
        sink.writeBytes(bytes, bytes.size)
        return bytes.size
    }
}