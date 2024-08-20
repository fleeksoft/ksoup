package com.fleeksoft.ksoup.kotlinx.ported.io

import com.fleeksoft.ksoup.ported.io.KByteBuffer
import com.fleeksoft.ksoup.ported.io.SourceReader
import io.ktor.utils.io.core.*
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlin.math.min

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
        return source().readByteArray(count)
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
        val bytes = this.readBytes(min(byteCount, source().remaining.toInt()))
        sink.writeBytes(bytes, bytes.size)
        return bytes.size
    }

    override val remaining: Long
        get() = source().remaining

    override fun peek(): SourceReader {
        return SourceReaderImpl(source().peek())
    }
}