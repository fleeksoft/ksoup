package com.fleeksoft.ksoup.ported.io

import korlibs.io.stream.SyncStream
import korlibs.io.stream.openSync
import korlibs.io.stream.readAll
import korlibs.io.stream.readBytes

interface BufferReader {
    public fun skip(count: Int)

    public fun mark(readLimit: Int)

    public fun reset()

    public fun readBytes(count: Int): ByteArray

    public fun read(): Byte

    public fun read(bytes: ByteArray, start: Int = 0, end: Int = bytes.size): Int

    public fun availableRead(): Long

    public fun readAllBytes(): ByteArray

    public fun isFullyRead(): Boolean

    public fun clone(): BufferReader

    public fun close()
}


class BufferReaderImpl : BufferReader {
    private val syncStream: SyncStream

    constructor(syncStream: SyncStream) {
        this.syncStream = syncStream
    }

    constructor(data: String) : this(data.openSync())

    constructor(bytes: ByteArray) : this(bytes.openSync())

    override fun skip(count: Int) {
        syncStream.skip(count)
    }

    override fun mark(readLimit: Int) {
        syncStream.mark(readLimit)
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

    override fun availableRead(): Long {
        return syncStream.availableRead
    }

    override fun readAllBytes(): ByteArray {
        return syncStream.readAll()
    }

    override fun isFullyRead(): Boolean {
        return syncStream.availableRead <= 0
    }

    override fun clone(): BufferReader {
        return BufferReaderImpl(syncStream.clone())
    }

    override fun close() {
        syncStream.close()
    }

}