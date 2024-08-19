package com.fleeksoft.ksoup.ported.io

interface SourceReader {
    public fun skip(count: Long)

    public fun mark(readLimit: Long)

    public fun reset()

    public fun readBytes(count: Int): ByteArray

    public fun read(): Byte

    public fun read(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int

    public fun readAllBytes(): ByteArray

    public fun exhausted(): Boolean

    public fun close()

    public fun readAtMostTo(sink: KByteBuffer, byteCount: Int): Int

    public val remaining: Long

    public fun peek(): SourceReader
}