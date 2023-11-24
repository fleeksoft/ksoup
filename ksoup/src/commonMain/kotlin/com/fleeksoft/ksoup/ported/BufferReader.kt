package com.fleeksoft.ksoup.ported

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import okio.Buffer
import okio.BufferedSource
import okio.Closeable
import okio.IOException

public open class BufferReader : Closeable {
    // TODO: optimize it may be using single bytearray
    // TODO: buffer reader size limit
    private val _source: BufferedSource
    private var closed: Boolean = false
    private var _charset: Charset? = null


    public constructor() {
        this._source = Buffer()
    }

    public constructor(buffer: BufferedSource) {
        this._source = buffer
    }

    // TODO: not sure if copy it or direct assign it
    public constructor(bufferReader: BufferReader) : this(bufferReader._source)

    public constructor(byteArray: ByteArray) {
        val buffer = Buffer()
        buffer.write(byteArray)
        _source = buffer
    }


    public constructor(data: String) : this(data.toByteArray())

    public fun remaining(): Int = getSource().buffer.size.toInt()

    public fun exhausted(): Boolean = getSource().exhausted()

    private fun getSource(): BufferedSource {
        if (closed) {
            throw IOException("Buffer closed!")
        }

        return _source
    }

    public fun getBuffer(): BufferReader {
        if (closed) {
            throw IOException("Buffer closed!")
        }

        return BufferReader(_source)
    }

    public fun size(): Long = _source.buffer.size

    public open fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        if (_charset != null) {
            val byteArray = ByteArray(sink.size)
            val result = getSource().read(sink = byteArray, offset = offset, byteCount = byteCount)
            io.ktor.utils.io.core.String(bytes = byteArray, charset = _charset!!).toByteArray().copyInto(sink)
            return result
        }
        return getSource().read(sink = sink, offset = offset, byteCount = byteCount)
    }

    public open fun read(): Int {
        return getSource().readByte().toInt()
    }

    public open fun readByteArray(): ByteArray {
        /*String(
                    bufferReader.readByteArray(),
                    charset = Charset.forName(charsetName)
                )*/
        var byteArray = getSource().readByteArray()
        if (_charset != null) {
            byteArray = io.ktor.utils.io.core.String(
                bytes = byteArray,
                charset = _charset!!
            ).toByteArray()
        }
        return byteArray
    }

    public fun skip(byteCount: Long) {
        getSource().skip(byteCount)
    }

    public fun skipFirstUnicodeChar(length: Int) {
        //        ignore x characters which can be 2-4 byte each for UTF-8
        val firstByte = getSource().peek().readByte().toInt()

        // Determine the length of the first character in UTF-8
        val firstCharLength = when {
            firstByte and 0x80 == 0 -> 1 // 0xxxxxxx, 1 byte
            firstByte and 0xE0 == 0xC0 -> 2 // 110xxxxx, 2 bytes
            firstByte and 0xF0 == 0xE0 -> 3 // 1110xxxx, 3 bytes
            firstByte and 0xF8 == 0xF0 -> 4 // 11110xxx, 4 bytes
            else -> 1
        }

        // Skip the first character and return the rest of the array
        getSource().skip((firstCharLength * length).toLong())
    }

    public fun getPeek(): BufferReader {
        val bufferReader = BufferReader(getSource().peek())
        bufferReader._charset = this._charset
        return bufferReader
    }

    public open fun read(b: ByteArray): Int = this[b]

    public operator fun get(pos: Int): Byte {
        return getSource().buffer[pos.toLong()]
    }

    public operator fun get(byteArray: ByteArray): Int {

        return getSource().read(byteArray)
    }


    public fun readString(): String {
        if (_charset != null) {
            return io.ktor.utils.io.core.String(bytes = getSource().readByteArray(), charset = _charset!!)
        }
        return getSource().readByteArray().decodeToString()
    }

    public fun readCharArray(): CharArray = readString().toCharArray()

    override fun close() {
        closed = true
        _source.close()
    }

    public fun setCharSet(charset: String) {
        this._charset = Charset.forName(charset)
    }
}