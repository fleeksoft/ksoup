package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.internal.SharedConstants
import kotlin.math.max
import kotlin.math.min

class BufferImpl : Buffer {
    private val buffer: ByteArray = ByteArray(SharedConstants.DEFAULT_BYTE_BUFFER_SIZE)
    private var position = 0
    private var readAvailable = 0
    private var offset = 0

    override val size: Int
        get() = buffer.size

    override fun position(): Int {
        return position
    }

    override fun available(): Int {
        return readAvailable
    }

    override fun compact() {
        if (position == buffer.size) {
            position = 0
            offset = 0
        } else if (position > 0) {
            val length = size - position
            (0 until length).forEach { i ->
                buffer[i] = buffer[i + position]
            }
            offset = length
            position = 0
        }
    }

    override fun exhausted(): Boolean {
        return position >= buffer.size
    }

    override fun readText(charset: Charset, max: Int): String {
        val endIndex = min(position + max, position + readAvailable)

        val byteArray = if (position == 0 && endIndex == buffer.size) {
            buffer
        } else {
            buffer.slice(position until endIndex).toByteArray()
        }

        val stringBuilder = StringBuilder()
        val consumedBytesCount = charset.decode(stringBuilder, byteArray, 0, byteArray.size)

        val string = stringBuilder.toString()
        if (consumedBytesCount > 0) {
            readAvailable -= consumedBytesCount
            position += consumedBytesCount
        }

        require(position <= size) { "read position overflow position: $position, bufferSize: $size" }
//        println("readText: position: $position, readAvailable: $readAvailable, offset: $offset, consumedBytesCount: $consumedBytesCount")
        return string
    }

    override fun writeBytes(byteArray: ByteArray, length: Int) {
//        println("writeBytes: $length")
        require(byteArray.size <= size)
        byteArray.copyInto(buffer, destinationOffset = offset, endIndex = length)
        readAvailable = min(SharedConstants.DEFAULT_BYTE_BUFFER_SIZE, readAvailable + byteArray.size)
        position = if (readAvailable == SharedConstants.DEFAULT_BYTE_BUFFER_SIZE) {
            0
        } else {
            max(0, position - byteArray.size)
        }
        offset += length
        if (offset >= buffer.size) {
            offset = 0
        }
    }

}