package com.fleeksoft.ksoup.ported

import io.ktor.utils.io.core.toByteArray
import okio.Buffer
import okio.BufferedSource
import okio.Closeable
import okio.IOException
import kotlin.math.min

public open class BufferReader : Closeable {
    // TODO: optimize it may be using single bytearray
    // TODO: buffer reader size limit
    private val originalBuffer: Buffer
    private lateinit var _currentBuffer: Buffer
    private var _markBuffer: Buffer? = null
    private var closed: Boolean = false


    public constructor() {
        this.originalBuffer = Buffer()
    }

    public constructor(bufferReader: BufferReader, maxSize: Int) {
        this.originalBuffer = Buffer()
        val source = bufferReader.getActiveBuffer()
        val size = if (maxSize == 0) {
            source.size
        } else {
            min(maxSize.toLong(), source.size)
        }
        source.copyTo(this.originalBuffer, offset = 0L, byteCount = size)
    }

    // TODO: not sure if copy it or direct assign it
    public constructor(buffer: Buffer) : this(buffer.peek().readByteArray())

    // TODO: not sure if copy it or direct assign it
    public constructor(bufferReader: BufferReader) : this(
        bufferReader.getActiveBuffer().peek().readByteArray()
    )

    public constructor(byteArray: ByteArray) {
        this.originalBuffer = Buffer()
        this.originalBuffer.write(byteArray)
    }


    public constructor(data: String) : this(data.toByteArray())

    public fun remaining(): Int = getActiveBuffer().buffer.size.toInt()

    public fun rewind() {
        reset()
        val currentBuffer = getCurrentBuffer()
        currentBuffer.clear()
        originalBuffer.copyTo(currentBuffer)
        _markBuffer = null
    }

    private fun getCurrentBuffer(): Buffer {
        if (!::_currentBuffer.isInitialized) {
            _currentBuffer = Buffer()
            originalBuffer.copyTo(_currentBuffer)
        }

        return _currentBuffer
    }


    public fun exhausted(): Boolean = getActiveBuffer().exhausted()

    public fun getActiveBuffer(): Buffer {
        if (closed) {
            throw IOException("Buffer closed!")
        }
        if (_markBuffer != null) {
            return _markBuffer!!
        }

        return getCurrentBuffer()
    }

    public fun size(): Long = getActiveBuffer().size

    public open fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        return getActiveBuffer().read(sink = sink, offset = offset, byteCount = byteCount)
    }

    public open fun read(): Int {
        return getActiveBuffer().readByte().toInt()
    }

    public open fun readByteArray(): ByteArray {
        return getActiveBuffer().readByteArray()
    }

    public fun skip(byteCount: Long) {
        getActiveBuffer().skip(byteCount)
    }

    public fun skipFirstUnicodeChar(length: Int) {
        //        ignore x characters which can be 2-4 byte each for UTF-8
        val firstByte = getActiveBuffer().peek().readByte().toInt()

        // Determine the length of the first character in UTF-8
        val firstCharLength = when {
            firstByte and 0x80 == 0 -> 1 // 0xxxxxxx, 1 byte
            firstByte and 0xE0 == 0xC0 -> 2 // 110xxxxx, 2 bytes
            firstByte and 0xF0 == 0xE0 -> 3 // 1110xxxx, 3 bytes
            firstByte and 0xF8 == 0xF0 -> 4 // 11110xxx, 4 bytes
            else -> 1
        }

        // Skip the first character and return the rest of the array
        getActiveBuffer().skip((firstCharLength * length).toLong())
    }

    public fun mark(readlimit: Int = 0) {
        val activeBuffer = getActiveBuffer()
        _markBuffer = Buffer()
        activeBuffer.copyTo(_markBuffer!!)
    }

    public fun getPeek(): BufferedSource {
        return getActiveBuffer().peek()
    }

    public fun reset() {
        _markBuffer = null
    }

    public open fun read(b: ByteArray): Int = this[b]

    public operator fun get(pos: Int): Byte {
        return getActiveBuffer().buffer[pos.toLong()]
    }

    public operator fun get(byteArray: ByteArray): Int {
        return getActiveBuffer().buffer.read(byteArray)
    }


    public fun readString(): String = getActiveBuffer().readByteArray().decodeToString()
    public fun readCharArray(): CharArray = readString().toCharArray()

    override fun close() {
        closed = true
        originalBuffer.clear()
        originalBuffer.close()
        _markBuffer?.close()
        if (::_currentBuffer.isInitialized) {
            _currentBuffer.close()
        }
    }
}