package com.fleeksoft.ksoup.ported.stream

import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.Charset
import kotlin.text.isNotEmpty
import kotlin.text.toCharArray

public class StreamCharReaderImpl(
    bufferReader: BufferReader,
    charset: Charset,
    chunkSize: Int,
) : StreamCharReader {
    private val charReader = CharReaderSyncStream(bufferReader = bufferReader, charset = charset, chunkSize = chunkSize)

    override fun skip(count: Int) {
        charReader.skip(count)
    }

    override fun mark(readLimit: Int) {
        charReader.mark(readLimit)
    }

    override fun reset() {
        charReader.reset()
    }

    override fun read(count: Int): String {
        val str = StringBuilder()
        charReader.read(str, count)
        return str.toString()
    }

    override fun readCharArray(
        charArray: CharArray,
        offset: Int,
        count: Int,
    ): Int {
        val strBuilder = StringBuilder()
        charReader.read(strBuilder, count)
        val str = strBuilder.toString()
        if (str.isNotEmpty()) {
            str.toCharArray().copyInto(charArray, destinationOffset = offset)
        } else if (count > 0) {
//            stream empty
            return -1
        }
        return str.length
    }
}