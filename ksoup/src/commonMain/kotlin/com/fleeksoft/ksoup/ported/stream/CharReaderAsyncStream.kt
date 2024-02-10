package com.fleeksoft.ksoup.ported.stream

import korlibs.datastructure.ByteArrayDeque
import korlibs.io.lang.Charset
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.readBytesUpTo

public class CharReaderAsyncStream(private val stream: AsyncStream, private val charset: Charset, private val chunkSize: Int = 1024) {
    private val temp = ByteArray(chunkSize)
    private val buffer = ByteArrayDeque()
    private var tempStringBuilder = StringBuilder()

    public fun clone(): CharReaderAsyncStream = CharReaderAsyncStream(stream.duplicate(), charset, chunkSize)

    public suspend fun read(
        out: StringBuilder,
        count: Int,
    ): Int {
        while (buffer.availableRead < temp.size) {
            val readCount = stream.readBytesUpTo(temp, 0, temp.size)
            if (readCount <= 0) break
            buffer.write(temp, 0, readCount)
        }

        while (tempStringBuilder.length < count) {
            val readCount = buffer.peek(temp)
            val consumed = charset.decode(tempStringBuilder, temp, 0, readCount)
            if (consumed <= 0) break
            buffer.skip(consumed)
        }

        // println("tempStringBuilder=$tempStringBuilder")

        val slice = tempStringBuilder.substring(0, kotlin.math.min(count, tempStringBuilder.length))
        tempStringBuilder = StringBuilder(slice.length).append(tempStringBuilder.substring(slice.length))

        out.append(slice)
        return slice.length
    }
}
