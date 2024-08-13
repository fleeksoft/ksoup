package com.fleeksoft.ksoup.ported.io

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.writeString

class BufferReaderImpl : BufferReader {
    private val syncStream: SourceMarker

    constructor(source: Source) {
        this.syncStream = SourceMarker(source)
    }

    constructor(data: String) : this(Buffer().apply { writeString(data) })

    constructor(bytes: ByteArray) : this(Buffer().apply { write(bytes) })

    override fun skip(count: Int) {
        syncStream.source().skip(count.toLong())
    }

    override fun mark(readLimit: Int) {
        syncStream.mark(readLimit.toLong())
    }

    override fun reset() {
        syncStream.reset()
    }

    override fun readBytes(count: Int): ByteArray {
        return syncStream.source().readByteArray(count)
    }

    override fun read(): Byte {
        return syncStream.source().readByte()
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        return syncStream.source().readAtMostTo(bytes, offset, length)
    }

    override fun readAllBytes(): ByteArray {
        return syncStream.source().readByteArray()
    }

    override fun exhausted(): Boolean {
        return syncStream.source().exhausted()
    }

    override fun clone(): BufferReader {
        return BufferReaderImpl(syncStream.source().peek())
    }

    override fun close() {
        syncStream.source().close()
    }

}