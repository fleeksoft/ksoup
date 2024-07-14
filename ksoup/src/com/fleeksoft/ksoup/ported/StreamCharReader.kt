package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.ported.stream.CharReaderSyncStream
import korlibs.io.lang.Charset
import korlibs.io.stream.SyncStream
import korlibs.io.stream.read

public interface StreamCharReader {
    public fun skip(count: Int)

    public fun mark(readLimit: Int)

    public fun reset()

    public fun read(count: Int): String

    public fun readCharArray(
        charArray: CharArray,
        offset: Int,
        count: Int,
    ): Int
}

public class StreamCharReaderImpl(
    stream: SyncStream,
    charset: Charset,
    chunkSize: Int,
) : StreamCharReader {
    private val charReader = CharReaderSyncStream(stream = stream, charset = charset, chunkSize = chunkSize)

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
