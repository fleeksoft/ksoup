package com.fleeksoft.ksoup.io

internal class SourceReaderByteArray(bytes: ByteArray) : SourceReader {
    private var source: ByteArray = bytes
    private var currentPosition: Int = 0
    private var markedPosition: Int? = null
    private var isClosed: Boolean = false

    override fun mark(readLimit: Long) {
        markedPosition = currentPosition
    }

    override fun reset() {
        isClosed = false
        markedPosition?.let {
            currentPosition = it
            markedPosition = null
        }
    }


    override fun readBytes(count: Int): ByteArray {
        val byteArray = ByteArray(count)
        var i = 0
        while (exhausted().not() && i < count) {
            byteArray[i] = source[currentPosition++]
            i++
        }
        return if (i == 0) {
            byteArrayOf()
        } else if (i != count) {
            byteArray.copyOfRange(0, i)
        } else {
            byteArray
        }
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        var i = offset
        var pos = currentPosition
        while (exhausted().not() && i < length) {
            bytes[i] = source[currentPosition++]
            i++
        }
        val totalRead = currentPosition - pos
        return if (totalRead == 0 && exhausted()) {
            -1
        } else {
            totalRead
        }
    }

    override fun readAllBytes(): ByteArray {
        return readBytes(source.size - currentPosition)
    }

    override fun exhausted(): Boolean {
        return currentPosition >= source.size
    }

    override fun close() {
//        on reset we need bytes again
//        source = ByteArray(0)
//        currentPosition = 0
//        markedPosition = null
        isClosed = true
    }
}