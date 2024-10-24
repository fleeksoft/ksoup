package com.fleeksoft.ksoup.io

import korlibs.io.stream.SyncStream
import korlibs.io.stream.openSync
import korlibs.io.stream.readAll
import korlibs.io.stream.readBytes


internal class SourceReaderImpl : SourceReader {
    private val syncStream: SyncStream

    constructor(syncStream: SyncStream) {
        this.syncStream = syncStream
    }

    constructor(bytes: ByteArray) : this(bytes.openSync())

    override fun mark(readLimit: Long) {
        syncStream.mark(readLimit.toInt())
    }

    override fun reset() {
        syncStream.reset()
    }

    override fun readInt(): Int {
        return syncStream.read()
    }

    override fun readBytes(count: Int): ByteArray {
        return syncStream.readBytes(count)
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        return syncStream.read(bytes, offset, length)
    }

    override fun readAllBytes(): ByteArray {
        return syncStream.readAll()
    }

    override fun exhausted(): Boolean {
        return syncStream.availableRead <= 0
    }

    override fun close() {
        syncStream.close()
    }

}