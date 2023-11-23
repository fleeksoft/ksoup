package com.fleeksoft.ksoup.ported

import io.ktor.utils.io.core.toByteArray
import okio.Buffer
import okio.BufferedSource
import okio.Closeable
import okio.IOException

public open class BufferReader : Closeable {
    // TODO: optimize it may be using single bytearray
    // TODO: buffer reader size limit
    private val source: BufferedSource
    private var closed: Boolean = false


    public constructor() {
        this.source = Buffer()
    }

    public constructor(buffer: BufferedSource) {
        this.source = buffer
    }

    // TODO: not sure if copy it or direct assign it
    public constructor(bufferReader: BufferReader) : this(bufferReader.source)

    public constructor(byteArray: ByteArray) {
        val buffer = Buffer()
        buffer.write(byteArray)
        source = buffer
    }


    public constructor(data: String) : this(data.toByteArray())

    public fun remaining(): Int = getActiveSource().buffer.size.toInt()

    public fun exhausted(): Boolean = getActiveSource().exhausted()

    public fun getActiveSource(): BufferedSource {
        if (closed) {
            throw IOException("Buffer closed!")
        }

        return source
    }

    public fun size(): Long = source.buffer.size

    public open fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        return getActiveSource().read(sink = sink, offset = offset, byteCount = byteCount)
    }

    public open fun read(): Int {
        return getActiveSource().readByte().toInt()
    }

    public open fun readByteArray(): ByteArray {
        return getActiveSource().readByteArray()
    }

    public fun skip(byteCount: Long) {
        getActiveSource().skip(byteCount)
    }

    public fun skipFirstUnicodeChar(length: Int) {
        //        ignore x characters which can be 2-4 byte each for UTF-8
        val firstByte = getActiveSource().peek().readByte().toInt()

        // Determine the length of the first character in UTF-8
        val firstCharLength = when {
            firstByte and 0x80 == 0 -> 1 // 0xxxxxxx, 1 byte
            firstByte and 0xE0 == 0xC0 -> 2 // 110xxxxx, 2 bytes
            firstByte and 0xF0 == 0xE0 -> 3 // 1110xxxx, 3 bytes
            firstByte and 0xF8 == 0xF0 -> 4 // 11110xxx, 4 bytes
            else -> 1
        }

        // Skip the first character and return the rest of the array
        getActiveSource().skip((firstCharLength * length).toLong())
    }

    public fun getPeek(): BufferedSource {
        return getActiveSource().peek()
    }

    public open fun read(b: ByteArray): Int = this[b]

    public operator fun get(pos: Int): Byte {
        return getActiveSource().buffer[pos.toLong()]
    }

    public operator fun get(byteArray: ByteArray): Int {

        return getActiveSource().read(byteArray)
    }


    public fun readString(): String = getActiveSource().readByteArray().decodeToString()
    public fun readCharArray(): CharArray = readString().toCharArray()

    override fun close() {
        closed = true
        source.close()
    }
}