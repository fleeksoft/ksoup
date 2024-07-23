package com.fleeksoft.ksoup.ported

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import okio.*
import okio.Buffer
import okio.Closeable
import okio.EOFException

public open class BufferReader : Closeable {
    private val _source: BufferedSource
    private var closed: Boolean = false
    private var _charset: Charset? = null

    public constructor() {
        this._source = Buffer()
    }

    public constructor(buffer: BufferedSource) {
        this._source = buffer
    }

    public constructor(source: Source) {
        this._source = source.buffer()
    }

    // TODO: not sure if copy it or direct assign it
    public constructor(bufferReader: BufferReader) : this(bufferReader._source)

    public constructor(byteArray: ByteArray, charset: String? = null) {
        val buffer = Buffer()
        buffer.write(byteArray)
        _source = buffer
        if (charset != null) {
            _charset = Charsets.forName(charset)
        }
    }

    public constructor(data: String, charset: String? = null) : this(data.toByteArray(), charset)

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

    public open fun read(
        sink: ByteArray,
        offset: Int,
        byteCount: Int,
    ): Int {
        var read = 0
        if (byteCount == 0 && getSource().exhausted()) return -1
        while (read < byteCount) {
            val readData = ByteArray(byteCount)
            val thisRead: Int = readInternal(readData, offset, byteCount - read) // okio limit max 8192
            if (thisRead > 0) {
                readData.copyOfRange(0, thisRead).copyInto(sink, destinationOffset = read)
            }
            if (thisRead == -1 && read == 0) {
                return -1
            }
            if (thisRead <= 0) break
            read += thisRead
        }

        return read
    }

    private fun readInternal(
        sink: ByteArray,
        offset: Int,
        byteCount: Int,
    ): Int {
        if (_charset != null) {
            val byteArray = ByteArray(sink.size)
            val result = getSource().read(sink = byteArray, offset = offset, byteCount = byteCount)
            if (result > 0) {
                String(bytes = byteArray.copyOfRange(0, result), charset = _charset!!).toByteArray().copyInto(sink)
            }
            return result
        }
        return getSource().read(sink = sink, offset = offset, byteCount = byteCount)
    }

    public open fun read(): Int {
        return getSource().readByte().toInt()
    }

    public open fun readByteArray(byteCount: Long? = null): ByteArray {
        var byteArray = if (byteCount != null) getSource().readByteArray(byteCount) else getSource().readByteArray()
        if (_charset != null && byteArray.isNotEmpty()) {
            byteArray =
                String(
                    bytes = byteArray,
                    charset = _charset!!,
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
        val firstCharLength = determineCharSize(firstByte)

        // Skip the first character and return the rest of the array
        getSource().skip((firstCharLength * length).toLong())
    }

    public fun peek(): BufferReader {
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

    public fun readCharArray(
        charArray: CharArray,
        off: Int = 0,
        len: Int,
        bytesSizeCallback: ((Int) -> Unit) = {},
    ): Int {
        if (len == 0) {
            bytesSizeCallback(0)
            return 0
        }
        var readBytes: Long = 0
        var readChars = 0
        val charToReadSize: Int = len

        val charset: Charset =
            if (_charset != null) {
                _charset!!
            } else {
                Charsets.UTF_8
            }
        val peekSource = getSource().peek()
        val tempBuffer = Buffer()
//                read buffer until required size or exhausted
        while (!peekSource.exhausted() && readBytes < charToReadSize) {
            readBytes += peekSource.read(tempBuffer, charToReadSize - readBytes)
        }

        var removeExtraChar = false
        while (!peekSource.exhausted()) {
            peekSource.peek().read(tempBuffer, 1)

//                jvm don't throw exception but nodejs is throwing exception here
            val str: String =
                runCatching {
                    io.ktor.utils.io.core.String(bytes = tempBuffer.peek().readByteArray(), charset = charset)
                }.getOrNull() ?: ""
//                    not sure how many bytes have this char for this encoding.
            if (str.length > charToReadSize) {
                removeExtraChar = true
                break
            } else {
                peekSource.skip(1)
                readBytes++
            }
        }
        if (readBytes > 0) {
            val strArray =
                io.ktor.utils.io.core.String(
                    bytes =
                        tempBuffer.readByteArray().let {
                            if (removeExtraChar) it.dropLast(1).toByteArray() else it
                        },
                    charset = charset,
                ).toCharArray()
            readChars = strArray.size
            strArray.copyInto(charArray, off)
            getSource().skip(readBytes)
        }

        if (charToReadSize > 0 && readBytes == 0L) {
            bytesSizeCallback(0)
            return -1
        }

        bytesSizeCallback(readBytes.toInt())
        return readChars
    }

    // TODO: need some improvements for unicode
    public fun readString(charCount: Long? = null): String {
        if (_charset != null && _charset != Charsets.UTF_8) {
            if (charCount != null) {
                val bytes: MutableList<Byte> = mutableListOf()
                while (!getSource().exhausted()) {
                    val byteArray = ByteArray(8192)
                    val readBytes = getSource().read(byteArray, 0, 8192)
                    if (readBytes > 0) {
                        bytes.addAll(byteArray.copyOfRange(0, readBytes).toList())
                    }
                    if (getSource().exhausted()) {
                        break
                    }

                    if (_charset != null) {
                        // TODO: need some improvements here:  _charset != null then counting by byte may be not correct if its unicode, in that case decode and then count it
                        if (io.ktor.utils.io.core.String(
                                bytes = bytes.toByteArray(),
                                charset = _charset!!,
                            ).length >= charCount
                        ) {
                            break
                        }
                    } else if (bytes.size >= charCount) {
                        break
                    }
                }
                return io.ktor.utils.io.core.String(bytes = bytes.toByteArray(), charset = _charset!!)
            } else {
                return io.ktor.utils.io.core.String(bytes = getSource().readByteArray(), charset = _charset!!)
            }
        } else {
            return if (charCount != null) {
                try {
                    getSource().readUtf8(charCount)
                } catch (ex: EOFException) {
                    getSource().readUtf8()
                }
            } else {
                getSource().readUtf8()
            }
        }
    }

    public fun readCharArray(): CharArray = readString().toCharArray()

    override fun close() {
        closed = true
        _source.close()
    }

    public fun setCharSet(charset: String) {
        this._charset = Charsets.forName(charset)
    }

    public companion object {
        public fun determineCharSize(byte: Int): Int {
            val firstCharLength =
                when {
                    byte and 0x80 == 0 -> 1 // 0xxxxxxx, 1 byte
                    byte and 0xE0 == 0xC0 -> 2 // 110xxxxx, 2 bytes
                    byte and 0xF0 == 0xE0 -> 3 // 1110xxxx, 3 bytes
                    byte and 0xF8 == 0xF0 -> 4 // 11110xxx, 4 bytes
                    else -> 1
                }
            return firstCharLength
        }
    }
}
