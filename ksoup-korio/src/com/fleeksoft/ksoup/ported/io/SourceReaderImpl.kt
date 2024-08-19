package com.fleeksoft.ksoup.ported.io

import korlibs.io.stream.SyncStream
import korlibs.io.stream.openSync
import korlibs.io.stream.readAll
import korlibs.io.stream.readBytes


class SourceReaderImpl : SourceReader {
    private val syncStream: SyncStream

    constructor(syncStream: SyncStream) {
        this.syncStream = syncStream
    }

    constructor(bytes: ByteArray) : this(bytes.openSync())

    override fun skip(count: Long) {
        syncStream.skip(count.toInt())
    }

    override fun mark(readLimit: Long) {
        syncStream.mark(readLimit.toInt())
    }

    override fun reset() {
        syncStream.reset()
    }

    override fun readBytes(count: Int): ByteArray {
        return syncStream.readBytes(count)
    }

    override fun read(): Byte {
        return syncStream.read().toByte()
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        return syncStream.read(bytes, offset, length)
    }

    /*override fun availableRead(): Long {
        return syncStream.availableRead
    }*/

    override fun readAllBytes(): ByteArray {
        return syncStream.readAll()
    }

    override fun exhausted(): Boolean {
        return syncStream.availableRead <= 0
    }

    override fun close() {
        syncStream.close()
    }

    override fun readAtMostTo(sink: KBuffer, byteCount: Int): Int {
        val bytes = syncStream.readBytes(byteCount)
        sink.writeBytes(bytes, bytes.size)
        return bytes.size
    }

    override val remaining: Long
        get() = syncStream.availableRead

    override fun peek(): SourceReader {
        return SourceReaderImpl(syncStream.clone())
    }

}