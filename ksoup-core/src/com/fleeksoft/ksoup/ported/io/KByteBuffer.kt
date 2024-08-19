package com.fleeksoft.ksoup.ported.io

import kotlin.math.min

class KByteBuffer(capacity: Int) {
    private val buffer: ByteArray = ByteArray(capacity)
    private var position = 0
    private var readAvailable = 0
    private var offset = 0

    val size: Int
        get() = buffer.size

    fun position(): Int {
        return position
    }

    fun available(): Int {
        return readAvailable
    }

    fun compact() {
        if (position == buffer.size || readAvailable == 0) {
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

    fun exhausted(): Boolean {
        return readAvailable <= 0
    }

    fun readText(charset: Charset, maxBytes: Int): String {
        val endIndex = min(position + maxBytes, position + readAvailable)

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

    fun writeBytes(byteArray: ByteArray, length: Int) {
//        println("writeBytes: $length")
        require(byteArray.size <= size)
        byteArray.copyInto(buffer, destinationOffset = offset, endIndex = length)
        readAvailable = min(buffer.size, readAvailable + byteArray.size)
        offset += length
        if (offset >= buffer.size) {
            offset = 0
        }
    }

}