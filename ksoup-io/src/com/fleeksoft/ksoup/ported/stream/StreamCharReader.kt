package com.fleeksoft.ksoup.ported.stream

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
