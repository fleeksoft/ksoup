package com.fleeksoft.ksoup.ported.stream

import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.Charset
import korlibs.io.stream.read
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
        return charReader.read(count)
    }

    override fun readCharArray(
        charArray: CharArray,
        offset: Int,
        count: Int,
    ): Int {
        val str = charReader.read(count)
        if (str.isNotEmpty()) {
            str.toCharArray().copyInto(charArray, destinationOffset = offset)
        } else if (count > 0) {
//            stream empty
            return -1
        }
        return str.length
    }
}