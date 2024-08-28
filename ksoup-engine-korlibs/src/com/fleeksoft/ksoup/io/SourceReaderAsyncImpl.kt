package com.fleeksoft.ksoup.io

import korlibs.io.stream.MarkableSyncInputStream
import korlibs.io.stream.SyncInputStream
import korlibs.io.stream.markable
import korlibs.io.stream.openSync
import korlibs.memory.buildByteArray


internal class SourceReaderAsyncImpl : SourceReader {
    private val stream: MarkableSyncInputStream
    private val buffer: KByteBuffer = KByteBuffer(8192)
    private var markBuffer: KByteBuffer? = null
    private var sourceEmpty = false

    constructor(stream: SyncInputStream) {
        this.stream = stream.markable()
    }

    constructor(bytes: ByteArray) : this(bytes.openSync())

    override fun mark(readLimit: Long) {
        markBuffer = buffer.clone()
        stream.mark(readLimit.toInt())
    }

    override fun reset() {
        markBuffer = null
        stream.reset()
    }

    private fun MarkableSyncInputStream.safeReadBytes(len: Int): ByteArray {
        var i = 0
        return buildByteArray {
            while (i < len) {
                var byte = this@safeReadBytes.read()
                if (byte == -1) byte = this@safeReadBytes.read()
                if (byte == -1) {
                    break
                } else {
                    this.append(byte)
                }
                i++
            }
        }
    }

    override fun readBytes(count: Int): ByteArray {
        return buildByteArray {
            if (buffer().available() > 0) {
                append(buffer().readBytes(count))
                buffer().compact()
            }
            if (!sourceEmpty && this.size < count) {
                append(stream.safeReadBytes(count - this.size))
            }
        }
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        val byteArray = readBytes(length)
        if (byteArray.isNotEmpty()) {
            byteArray.copyInto(bytes, offset, 0, offset + length)
        }
        return byteArray.size
    }

    private fun buffer() = markBuffer ?: buffer

    override fun readAllBytes(): ByteArray {
        return buildByteArray {
            if (buffer().available() > 0) {
                append(buffer().readAll())
                buffer().compact()
            }
            while (true) {
                val byte = stream.read()
                if (byte != -1) {
                    this.append(byte.toByte())
                } else {
                    break;
                }
            }
        }
    }

    override fun exhausted(): Boolean {
        if (buffer().available() > 0) return false
        if (sourceEmpty) return true
        val len = buffer().size
        val bytes = stream.safeReadBytes(len)
        if (bytes.size < len) {
            sourceEmpty = true
        }
        buffer().writeBytes(bytes, bytes.size)
        return (buffer().available() > 0).not()
    }

    override fun close() {
        stream.close()
    }

    override fun readAtMostTo(sink: KByteBuffer, byteCount: Int): Int {
        val bytes = this.readBytes(byteCount)
        sink.writeBytes(bytes, bytes.size)
        return bytes.size
    }

}