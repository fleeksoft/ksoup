package com.fleeksoft.ksoup.kotlinx.ported.io

import com.fleeksoft.ksoup.ported.io.BufferReader
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.writeString

class BufferReaderImpl : BufferReader {
    private val source: Source
    private var markSource: Source? = null

    constructor(source: Source) {
        this.source = source
    }

    constructor(data: String) : this(Buffer().apply { writeString(data) })

    constructor(bytes: ByteArray) : this(Buffer().apply { write(bytes) })

    private fun source(): Source = markSource ?: source

    override fun skip(count: Int) {
        source().skip(count.toLong())
    }

    override fun mark(readLimit: Int) {
        markSource = source().peek()
    }

    override fun reset() {
        markSource = null
    }

    override fun readBytes(count: Int): ByteArray {
        return source().readByteArray(count)
    }

    override fun read(): Byte {
        return source().readByte()
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        return source().readAtMostTo(bytes, offset, length)
    }

    override fun readAllBytes(): ByteArray {
        return source().readByteArray()
    }

    override fun exhausted(): Boolean {
        return source().exhausted()
    }

    override fun clone(): BufferReader {
        return BufferReaderImpl(source().peek())
    }

    override fun close() {
        source.close()
        markSource?.close()
    }

}